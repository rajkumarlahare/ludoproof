[CmdletBinding()]
param(
    [switch]$Verify
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent $PSScriptRoot
$androidDir = Join-Path $repoRoot "android"
$packageName = "com.ludoproof.game"
$activityName = "com.ludoproof.game.HomeActivity"
$gradleVersion = "8.9"
$requiredPlatform = "android-35"
$requiredBuildTools = "34.0.0"

function Use-SystemJava {
    $javaExe = $null

    if ($env:JAVA_HOME) {
        $candidate = Join-Path $env:JAVA_HOME "bin\java.exe"
        if (Test-Path $candidate) {
            $javaExe = $candidate
        }
    }

    if (!$javaExe) {
        $java = Get-Command java.exe -ErrorAction SilentlyContinue
        if (!$java) {
            throw "Java was not found. LudoProof uses the same system Java setup as FinWorker."
        }

        $javaExe = $java.Source
        $env:JAVA_HOME = Split-Path -Parent (Split-Path -Parent $javaExe)
    }

    $env:Path = (Join-Path $env:JAVA_HOME "bin") + ";" + $env:Path

    Write-Host "Using system Java from $env:JAVA_HOME"
    if ($Verify) {
        & $javaExe -version
        if ($LASTEXITCODE -ne 0) {
            throw "Java could not be started from $javaExe"
        }
    }
}

function Test-AndroidSdkRoot(
    [string]$sdkRoot
) {
    if ([string]::IsNullOrWhiteSpace($sdkRoot)) {
        return $false
    }

    if (!(Test-Path $sdkRoot)) {
        return $false
    }

    $adb = Join-Path $sdkRoot "platform-tools\adb.exe"
    $platform = Join-Path $sdkRoot ("platforms\" + $requiredPlatform)
    $buildTools = Join-Path $sdkRoot ("build-tools\" + $requiredBuildTools)

    return (
        (Test-Path $adb) -and
        (Test-Path $platform) -and
        (Test-Path $buildTools)
    )
}

function Resolve-AndroidSdkRoot {
    $candidates = @()

    if ($env:LUDOPROOF_ANDROID_SDK) {
        $candidates += $env:LUDOPROOF_ANDROID_SDK
    }

    $repoDrive = [System.IO.Path]::GetPathRoot($repoRoot)
    if ($repoDrive) {
        $candidates += (Join-Path $repoDrive "Dev\finworkar-tools\android-sdk")
    }

    if ($env:ANDROID_SDK_ROOT) {
        $candidates += $env:ANDROID_SDK_ROOT
    }

    if ($env:ANDROID_HOME) {
        $candidates += $env:ANDROID_HOME
    }

    if ($env:LOCALAPPDATA) {
        $candidates += (Join-Path $env:LOCALAPPDATA "Android\Sdk")
    }

    foreach ($candidate in $candidates | Select-Object -Unique) {
        if (Test-AndroidSdkRoot $candidate) {
            return (Resolve-Path $candidate).Path
        }
    }

    $checked = ($candidates | Select-Object -Unique) -join [Environment]::NewLine
    throw @"
A complete Android SDK for LudoProof was not found.

LudoProof requires:
  platforms\$requiredPlatform
  build-tools\$requiredBuildTools
  platform-tools\adb.exe

Checked:
$checked

You can explicitly set LUDOPROOF_ANDROID_SDK to your working Android SDK path.
"@
}

function Ensure-AndroidSdkProperties(
    [string]$sdkRoot
) {
    $localProperties = Join-Path $androidDir "local.properties"
    $portableSdkPath = $sdkRoot -replace '\\', '/'
    $desired = "sdk.dir=$portableSdkPath"

    $current =
        if (Test-Path $localProperties) {
            (Get-Content -Raw -Path $localProperties).Trim()
        } else {
            ""
        }

    if ($current -ne $desired) {
        Set-Content -Encoding ASCII -Path $localProperties -Value $desired
        Write-Host "Updated android/local.properties"
    }

    $env:ANDROID_SDK_ROOT = $sdkRoot
    $env:ANDROID_HOME = $sdkRoot

    Write-Host "Using Android SDK from $sdkRoot"
}

function Resolve-Adb(
    [string]$sdkRoot
) {
    $adb = Join-Path $sdkRoot "platform-tools\adb.exe"
    if (!(Test-Path $adb)) {
        throw "ADB was not found in the resolved Android SDK: $adb"
    }

    return $adb
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
        }
    }

    foreach ($url in $urls) {
        for ($attempt = 1; $attempt -le 3; $attempt += 1) {
            try {
                Write-Host "PowerShell Gradle download attempt $attempt from $url"
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
                Write-Host ("Gradle download attempt " + $attempt + " failed: " + $_.Exception.Message)
            }

            Remove-Item -Force -ErrorAction SilentlyContinue $partialPath
            Start-Sleep -Seconds ([Math]::Min(2 * $attempt, 6))
        }
    }

    throw "Unable to download Gradle $gradleVersion."
}

function Resolve-Gradle {
    $toolsDir = Join-Path $repoRoot ".tools"
    $gradleHome = Join-Path $toolsDir "gradle-$gradleVersion"
    $gradleBat = Join-Path $gradleHome "bin\gradle.bat"

    if (Test-Path $gradleBat) {
        return $gradleBat
    }

    New-Item -ItemType Directory -Force -Path $toolsDir | Out-Null
    $zipPath = Join-Path $toolsDir "gradle-$gradleVersion-bin.zip"

    Write-Host ""
    Write-Host "Gradle $gradleVersion is not cached. Downloading it once for LudoProof..."
    Download-GradleDistribution $zipPath

    Remove-Item -Recurse -Force -ErrorAction SilentlyContinue $gradleHome

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

Use-SystemJava

$sdkRoot = Resolve-AndroidSdkRoot
Ensure-AndroidSdkProperties $sdkRoot

$adb = Resolve-Adb $sdkRoot

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
if ($Verify) {
    Write-Host "Verifying, building and installing LudoProof..."
    & $gradle -p $androidDir :app:testDebugUnitTest :app:lintDebug :app:installDebug --console=plain --build-cache --parallel
} else {
    Write-Host "Fast incremental build + install..."
    & $gradle -p $androidDir :app:installDebug --console=plain --build-cache --parallel
}
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
