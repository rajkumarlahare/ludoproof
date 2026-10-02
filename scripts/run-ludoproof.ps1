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

$java = Get-Command java -ErrorAction SilentlyContinue
if (!$java) {
    throw "Java was not found. Set JAVA_HOME or add your JDK bin folder to PATH."
}

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
& $gradle -p $androidDir :app:installDebug --console=plain
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
