[CmdletBinding()]
param(
    [string]$AppVersion = "2.0.0",
    [switch]$Installer,
    [switch]$SkipBuild
)

$ErrorActionPreference = "Stop"
$Root = Split-Path -Parent $PSScriptRoot
Set-Location $Root

function Fail {
    param([string]$Message)
    Write-Host "ERROR: $Message" -ForegroundColor Red
    exit 1
}

function Find-JdkHome {
    $candidates = @()
    if ($env:JAVA_HOME) {
        $candidates += $env:JAVA_HOME
    }
    $candidates += @(
        "D:\Programing_materials\Java\JDK26",
        "C:\Program Files\Java\jdk-26",
        "C:\Program Files\Eclipse Adoptium\jdk-26*",
        "C:\Program Files\Microsoft\jdk-26*"
    )
    $java = Get-Command java.exe -ErrorAction SilentlyContinue
    if ($java -and $java.Source) {
        $candidates += Split-Path -Parent (Split-Path -Parent $java.Source)
    }
    foreach ($candidate in $candidates) {
        if (-not $candidate) {
            continue
        }
        $resolved = @(Get-Item $candidate -ErrorAction SilentlyContinue)
        foreach ($item in $resolved) {
            $jdkDir = $item.FullName
            if ((Test-Path (Join-Path $jdkDir "bin\java.exe")) -and (Test-Path (Join-Path $jdkDir "bin\jpackage.exe"))) {
                return $jdkDir
            }
        }
    }
    Fail "JDK 26 was not found. Install a full JDK 26 or set JAVA_HOME to its installation directory."
}

function Find-Maven {
    if (Get-Command mvn -ErrorAction SilentlyContinue) {
        return "mvn"
    }
    $candidates = @(
        "D:\Programing_materials\Java\marven\apache-maven-3.9.12\bin\mvn.cmd",
        "C:\Program Files\Apache\maven\bin\mvn.cmd"
    )
    foreach ($candidate in $candidates) {
        if (Test-Path $candidate) {
            return $candidate
        }
    }
    Fail "Maven was not found. Install Maven and add mvn to PATH."
}

$jdkHome = Find-JdkHome
$javaExe = Join-Path $jdkHome "bin\java.exe"
$jpackageExe = Join-Path $jdkHome "bin\jpackage.exe"

$versionText = (& $javaExe -version 2>&1 | Out-String)
if ($versionText -notmatch 'version "26') {
    Fail "JDK 26 is required. Detected: $versionText"
}
if (-not (Test-Path $jpackageExe)) {
    Fail "jpackage was not found in $jdkHome. Install a full JDK 26, not a JRE."
}

$maven = Find-Maven
Write-Host "JDK:    $jdkHome"
Write-Host "Maven:  $maven"

if (-not $SkipBuild) {
    Write-Host "Building application jar..."
    & $maven -q -DskipTests clean package
    if ($LASTEXITCODE -ne 0) {
        Fail "Maven build failed."
    }
}

$jar = Get-Item "target\Time_Yourself-2.0.jar" -ErrorAction SilentlyContinue
if (-not $jar) {
    $jar = Get-ChildItem "target" -Filter "Time_Yourself-*.jar" -ErrorAction SilentlyContinue |
        Where-Object { $_.Name -notlike "*.original" } |
        Select-Object -First 1
}
if (-not $jar) {
    Fail "The application jar was not found under target."
}

$modules = "java.base,java.compiler,java.desktop,java.instrument,java.logging,java.management,java.naming,java.net.http,java.prefs,java.rmi,java.security.jgss,java.security.sasl,java.sql,java.sql.rowset,java.transaction.xa,java.xml,java.xml.crypto,jdk.crypto.ec,jdk.localedata,jdk.unsupported,jdk.zipfs"

$outRoot = Join-Path $Root "build\windows"
$inputDir = Join-Path $outRoot "input"
$distDir = Join-Path $outRoot "dist"
$installerDir = Join-Path $outRoot "installer"

Remove-Item -Recurse -Force $outRoot -ErrorAction SilentlyContinue
New-Item -ItemType Directory -Force $inputDir | Out-Null
Copy-Item $jar.FullName (Join-Path $inputDir $jar.Name)

$icon = Join-Path $Root "appLogo.ico"
if (-not (Test-Path $icon)) {
    $logo = Join-Path $Root "appLogo.png"
    $python = Get-Command python.exe -ErrorAction SilentlyContinue
    if ((Test-Path $logo) -and $python) {
        Write-Host "Generating appLogo.ico from appLogo.png..."
        & $python.Source -c "from PIL import Image; Image.open('appLogo.png').convert('RGBA').save('appLogo.ico', sizes=[(16,16),(24,24),(32,32),(48,48),(64,64),(128,128),(256,256)])"
        if ($LASTEXITCODE -ne 0) {
            Write-Host "WARNING: icon generation failed. Packaging without a custom icon." -ForegroundColor Yellow
            $icon = $null
        }
    } else {
        Write-Host "WARNING: appLogo.ico/appLogo.png not found. Packaging without a custom icon." -ForegroundColor Yellow
        $icon = $null
    }
}

$commonArgs = @(
    "--type", "app-image",
    "--name", "TimeYourself",
    "--app-version", $AppVersion,
    "--vendor", "Harry-1391453181620 Developer Group",
    "--description", "Time Yourself - offline task manager",
    "--copyright", "Harry-1391453181620 Developer Group",
    "--input", $inputDir,
    "--main-jar", $jar.Name,
    "--main-class", "org.springframework.boot.loader.launch.JarLauncher",
    "--dest", $distDir,
    "--add-modules", $modules,
    "--java-options", "-Dfile.encoding=UTF-8",
    "--java-options", "-Dstdout.encoding=UTF-8",
    "--java-options", "-Dstderr.encoding=UTF-8",
    "--java-options", "--enable-native-access=ALL-UNNAMED",
    "--java-options", "-Dserver.port=0"
)
if ($icon) {
    $commonArgs += @("--icon", $icon)
}

Write-Host "Building portable app image..."
& $jpackageExe @commonArgs
if ($LASTEXITCODE -ne 0) {
    Fail "jpackage app-image build failed."
}

$appExe = Join-Path $distDir "TimeYourself\TimeYourself.exe"
if (-not (Test-Path $appExe)) {
    Fail "Expected application executable was not produced: $appExe"
}

$infoFile = Join-Path $outRoot "build-info.txt"
$jarHash = (Get-FileHash $jar.FullName -Algorithm SHA256).Hash
@(
    "app=TimeYourself",
    "version=$AppVersion",
    "built_at=$((Get-Date).ToString("o"))",
    "jar=$($jar.Name)",
    "jar_sha256=$jarHash",
    "app_image=build\windows\dist\TimeYourself"
) | Set-Content -Encoding UTF8 $infoFile

Write-Host "Portable app image: $appExe"

if ($Installer) {
    $wixBin = $null
    $candle = Get-Command candle.exe -ErrorAction SilentlyContinue
    if ($candle) {
        $wixBin = Split-Path -Parent $candle.Source
    } else {
        $candidates = @(
            "C:\Program Files (x86)\WiX Toolset v3.14\bin",
            "C:\Program Files\WiX Toolset v3.14\bin"
        )
        foreach ($candidate in $candidates) {
            if (Test-Path (Join-Path $candidate "candle.exe")) {
                $wixBin = $candidate
                break
            }
        }
    }
    if (-not $wixBin) {
        Fail "WiX Toolset 3.14 is required for the .exe installer. Download it from https://github.com/wixtoolset/wix3/releases and rerun with -Installer."
    }
    $env:Path = "$wixBin;$env:Path"

    Write-Host "Building Windows installer (per-machine, directory selectable)..."
    $installerArgs = @(
        "--type", "exe",
        "--name", "TimeYourself",
        "--app-version", $AppVersion,
        "--vendor", "Harry-1391453181620 Developer Group",
        "--description", "Time Yourself - offline task manager",
        "--copyright", "Harry-1391453181620 Developer Group",
        "--input", $inputDir,
        "--main-jar", $jar.Name,
        "--main-class", "org.springframework.boot.loader.launch.JarLauncher",
        "--dest", $installerDir,
        "--add-modules", $modules,
        "--java-options", "-Dfile.encoding=UTF-8",
        "--java-options", "-Dstdout.encoding=UTF-8",
        "--java-options", "-Dstderr.encoding=UTF-8",
        "--java-options", "--enable-native-access=ALL-UNNAMED",
        "--java-options", "-Dserver.port=0",
        "--win-dir-chooser",
        "--win-menu",
        "--win-shortcut"
    )
    if ($icon) {
        $installerArgs += @("--icon", $icon)
    }
    & $jpackageExe @installerArgs
    if ($LASTEXITCODE -ne 0) {
        Fail "jpackage installer build failed."
    }
    $installerPath = Join-Path $installerDir "TimeYourself-$AppVersion.exe"
    if (-not (Test-Path $installerPath)) {
        Fail "Expected installer was not produced: $installerPath"
    }
    Write-Host "Installer:           $installerPath"
}

Write-Host ""
Write-Host "DONE" -ForegroundColor Green
Write-Host "Portable app:  $appExe"
if ($Installer) {
    Write-Host "Installer:     $(Join-Path $installerDir ("TimeYourself-$AppVersion.exe"))"
}
Write-Host "Build info:    $infoFile"
Write-Host ""
Write-Host "The packaged app stores its data next to the executable in data\timeyourself.db."
Write-Host "The database is created automatically on first launch."
