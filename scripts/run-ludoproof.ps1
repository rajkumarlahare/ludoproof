Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent $PSScriptRoot
$androidDir = Join-Path $repoRoot "android"
$packageName = "com.ludoproof.game"
$activityName = "com.ludoproof.game.HomeActivity"
$gradleVersion = "8.9"

function Resolve-Adb {
    $command = Get-Command adb -ErrorAction SilentlyContinue
    if ($command) {
        return $command.Source
    }

    $sdkRoots = @()
    if ($env:ANDROID_SDK_ROOT) {
        $sdkRoots += $env:ANDROID_SDK_ROOT
    }
    if ($env:ANDROID_HOME) {
        $sdkRoots += $env:ANDROID_HOME
    }
    if ($env:LOCALAPPDATA) {
        $sdkRoots += (Join-Path $env:LOCALAPPDATA "Android\Sdk")
    }

    foreach ($sdkRoot in $sdkRoots | Select-Object -Unique) {
        $candidate = Join-Path $sdkRoot "platform-tools\adb.exe"
        if (Test-Path $candidate) {
            return $candidate
        }
    }

    throw "ADB not found. Install Android SDK Platform-Tools or set ANDROID_SDK_ROOT."
}

function Download-GradleDistribution(
    [string]$destination
) {
    $urls = @(
        "https://downloads.gradle.org/distributions/gradle-$gradleVersion-bin.zip",
        "https://services.gradle.org/distributions/gradle-$gradleVersion-bin.zip"
    )
    $partialPath = "$destination.part"

    Remove-Item -Force -ErrorAction SilentlyContinue $destination
    Remove-Item -Force -ErrorAction SilentlyContinue $partialPath

    $curl = Get-Command curl.exe -ErrorAction SilentlyContinue
    if ($curl) {
        foreach ($url in $urls) {
            Write-Host "Downloading Gradle from $url"
            $curlArgs = @(
                "--fail",
                "--location",
                "--retry", "4",
                "--retry-delay", "2",
                "--retry-all-errors",
                "--connect-timeout", "20",
                "--max-time", "900",
                "--output", $partialPath,
                $url
            )
            & $curl.Source @curlArgs

            if (
                $LASTEXITCODE -eq 0 -and
                (Test-Path $partialPath) -and
                (Get-Item $partialPath).Length -gt 50MB
            ) {
                Move-Item -Force $partialPath $destination
                return
            }

            Remove-Item -Force -ErrorAction SilentlyContinue $partialPath
            Write-Host "Download attempt failed. Trying the next Gradle source..."
        }
    }

    foreach ($url in $urls) {
        for ($attempt = 1; $attempt -le 3; $attempt += 1) {
            try {
                Write-Host "PowerShell download attempt $attempt from $url"
                [Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
                Invoke-WebRequest -UseBasicParsing -TimeoutSec 900 -Uri $url -OutFile $partialPath

                if (
                    (Test-Path $partialPath) -and
                    (Get-Item $partialPath).Length -gt 50MB
                ) {
                    Move-Item -Force $partialPath $destination
                    return
                }
            } catch {
                Write-Host ("Download attempt " + $attempt + " failed: " + $_.Exception.Message)
            }

            Remove-Item -Force -ErrorAction SilentlyContinue $partialPath
            Start-Sleep -Seconds ([Math]::Min(2 * $attempt, 6))
        }
    }

    throw "Unable to download Gradle $gradleVersion. Check internet access and try Ctrl+Shift+B again."
}

function Resolve-Gradle {
    $command = Get-Command gradle -ErrorAction SilentlyContinue
    if ($command) {
        return $command.Source
    }

    $toolsDir = Join-Path $repoRoot ".tools"
    $gradleHome = Join-Path $toolsDir "gradle-$gradleVersion"
    $gradleBat = Join-Path $gradleHome "bin\gradle.bat"

    if (Test-Path $gradleBat) {
        return $gradleBat
    }

    New-Item -ItemType Directory -Force -Path $toolsDir | Out-Null
    $zipPath = Join-Path $toolsDir "gradle-$gradleVersion-bin.zip"

    Write-Host ""
    Write-Host "Gradle $gradleVersion is not installed. Downloading it once for LudoProof..."
    Download-GradleDistribution $zipPath

    if (Test-Path $gradleHome) {
        Remove-Item -Recurse -Force $gradleHome
    }

    try {
        Expand-Archive -Path $zipPath -DestinationPath $toolsDir -Force
    } finally {
        Remove-Item -Force -ErrorAction SilentlyContinue $zipPath
    }

    if (!(Test-Path $gradleBat)) {
        throw "Gradle bootstrap failed: $gradleBat was not created."
    }

    return $gradleBat
}

function Test-Java17Home(
    [string]$javaHome
) {
    if ([string]::IsNullOrWhiteSpace($javaHome)) {
        return $false
    }

    $javaExe = Join-Path $javaHome "bin\java.exe"
    $releaseFile = Join-Path $javaHome "release"

    if (
        !(Test-Path $javaExe) -or
        !(Test-Path $releaseFile)
    ) {
        return $false
    }

    try {
        $releaseText = Get-Content -Raw -Path $releaseFile
        return $releaseText -match '(?m)^JAVA_VERSION="17(?:\.|")'
    } catch {
        return $false
    }
}

function Find-Java17HomeUnder(
    [string]$root
) {
    if ([string]::IsNullOrWhiteSpace($root)) {
        return $null
    }

    if (Test-Java17Home $root) {
        return $root
    }

    if (!(Test-Path $root)) {
        return $null
    }

    $releaseFiles = @(
        Get-ChildItem -Path $root -Filter "release" -File -Recurse -ErrorAction SilentlyContinue
    )

    foreach ($releaseFile in $releaseFiles) {
        $candidate = $releaseFile.DirectoryName
        if (Test-Java17Home $candidate) {
            return $candidate
        }
    }

    return $null
}

function Download-Java17Distribution(
    [string]$destination
) {
    $urls = @(
        "https://api.adoptium.net/v3/binary/latest/17/ga/windows/x64/jdk/hotspot/normal/eclipse"
    )
    $partialPath = "$destination.part"

    if (
        (Test-Path $destination) -and
        (Get-Item $destination).Length -gt 50MB
    ) {
        Write-Host "Reusing existing Temurin JDK 17 archive."
        return
    }

    Remove-Item -Force -ErrorAction SilentlyContinue $destination
    Remove-Item -Force -ErrorAction SilentlyContinue $partialPath

    $curl = Get-Command curl.exe -ErrorAction SilentlyContinue
    if ($curl) {
        foreach ($url in $urls) {
            Write-Host "Downloading Temurin JDK 17..."
            $curlArgs = @(
                "--fail",
                "--location",
                "--retry", "4",
                "--retry-delay", "2",
                "--retry-all-errors",
                "--connect-timeout", "20",
                "--max-time", "1200",
                "--output", $partialPath,
                $url
            )
            & $curl.Source @curlArgs

            if (
                $LASTEXITCODE -eq 0 -and
                (Test-Path $partialPath) -and
                (Get-Item $partialPath).Length -gt 50MB
            ) {
                Move-Item -Force $partialPath $destination
                return
            }

            Remove-Item -Force -ErrorAction SilentlyContinue $partialPath
        }
    }

    foreach ($url in $urls) {
        for ($attempt = 1; $attempt -le 3; $attempt += 1) {
            try {
                Write-Host "PowerShell JDK 17 download attempt $attempt..."
                [Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
                Invoke-WebRequest -UseBasicParsing -TimeoutSec 1200 -Uri $url -OutFile $partialPath

                if (
                    (Test-Path $partialPath) -and
                    (Get-Item $partialPath).Length -gt 50MB
                ) {
                    Move-Item -Force $partialPath $destination
                    return
                }
            } catch {
                Write-Host ("JDK 17 download attempt " + $attempt + " failed: " + $_.Exception.Message)
            }

            Remove-Item -Force -ErrorAction SilentlyContinue $partialPath
            Start-Sleep -Seconds ([Math]::Min(2 * $attempt, 6))
        }
    }

    throw "Unable to download Temurin JDK 17. Check internet access and try Ctrl+Shift+B again."
}

function Resolve-Java17 {
    $candidates = @()

    if ($env:JAVA_HOME) {
        $candidates += $env:JAVA_HOME
    }

    $currentJava = Get-Command java.exe -ErrorAction SilentlyContinue
    if ($currentJava) {
        $candidates += (
            Split-Path -Parent (
                Split-Path -Parent $currentJava.Source
            )
        )
    }

    $roots = @(
        "C:\Program Files\Eclipse Adoptium",
        "C:\Program Files\Java",
        "C:\Program Files\Microsoft",
        (Join-Path $env:USERPROFILE ".jdks")
    )

    foreach ($candidate in $candidates | Select-Object -Unique) {
        $found = Find-Java17HomeUnder $candidate
        if ($found) {
            return $found
        }
    }

    foreach ($root in $roots) {
        $found = Find-Java17HomeUnder $root
        if ($found) {
            return $found
        }
    }

    $toolsDir = Join-Path $repoRoot ".tools"
    $javaCacheRoot = Join-Path $toolsDir "temurin-17"

    $cached = Find-Java17HomeUnder $javaCacheRoot
    if ($cached) {
        Write-Host "Reusing cached Java 17 from $cached"
        return $cached
    }

    New-Item -ItemType Directory -Force -Path $toolsDir | Out-Null

    $legacyExtractRoot = Join-Path $toolsDir "temurin-17-extract"
    $legacyCached = Find-Java17HomeUnder $legacyExtractRoot
    if ($legacyCached) {
        Write-Host "Reusing previously extracted Java 17 from $legacyCached"
        return $legacyCached
    }

    $zipPath = Join-Path $toolsDir "temurin-17-jdk.zip"

    Write-Host ""
    Write-Host "Java 17 is required by LudoProof. Installing a private Temurin 17 copy once..."
    Download-Java17Distribution $zipPath

    Remove-Item -Recurse -Force -ErrorAction SilentlyContinue $javaCacheRoot
    New-Item -ItemType Directory -Force -Path $javaCacheRoot | Out-Null

    try {
        Expand-Archive -Path $zipPath -DestinationPath $javaCacheRoot -Force
        $installed = Find-Java17HomeUnder $javaCacheRoot

        if (!$installed) {
            throw "Temurin JDK 17 archive was extracted, but no valid JDK 17 home was found."
        }

        Write-Host "Java 17 installed at $installed"
        return $installed
    } catch {
        Remove-Item -Recurse -Force -ErrorAction SilentlyContinue $javaCacheRoot
        throw
    } finally {
        $validCache = Find-Java17HomeUnder $javaCacheRoot
        if ($validCache) {
            Remove-Item -Force -ErrorAction SilentlyContinue $zipPath
        }
    }
}

function Use-Java17 {
    $javaHome = Resolve-Java17
    $env:JAVA_HOME = $javaHome
    $env:Path = (Join-Path $javaHome "bin") + ";" + $env:Path
    Write-Host "Using Java 17 from $javaHome"
}

function Ensure-AndroidSdkProperties([string]$adbPath) {
    $platformToolsDir = Split-Path -Parent $adbPath
    $sdkRoot = Split-Path -Parent $platformToolsDir
    $localProperties = Join-Path $androidDir "local.properties"

    if (!(Test-Path $localProperties)) {
        $portableSdkPath = $sdkRoot.Replace("\", "/")
        "sdk.dir=$portableSdkPath" | Set-Content -Encoding ASCII -Path $localProperties
        Write-Host "Created android/local.properties for $sdkRoot"
    }
}

Use-Java17

$adb = Resolve-Adb
Ensure-AndroidSdkProperties $adb

$deviceLines = @(
    (& $adb devices) |
        Select-Object -Skip 1 |
        Where-Object { $_ -match "\tdevice$" }
)

if ($deviceLines.Count -eq 0) {
    throw "No authorized Android phone found. Connect USB, enable USB debugging, and approve the RSA prompt on the phone."
}

$adbArgs = @()
if ($env:ANDROID_SERIAL) {
    $adbArgs = @("-s", $env:ANDROID_SERIAL)
} elseif ($deviceLines.Count -gt 1) {
    throw "More than one Android device is connected. Set ANDROID_SERIAL to choose one device."
}

$gradle = Resolve-Gradle

Write-Host ""
Write-Host "Building and installing LudoProof..."
& $gradle -p $androidDir :app:installDebug --console=plain -Dorg.gradle.java.installations.paths="$env:JAVA_HOME"
if ($LASTEXITCODE -ne 0) {
    exit $LASTEXITCODE
}

Write-Host ""
Write-Host "Launching LudoProof on the connected phone..."
& $adb @adbArgs shell am force-stop $packageName | Out-Null
& $adb @adbArgs shell am start -n "$packageName/$activityName"

if ($LASTEXITCODE -ne 0) {
    exit $LASTEXITCODE
}

Write-Host ""
Write-Host "LudoProof installed and launched successfully."
