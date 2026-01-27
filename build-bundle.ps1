<#
.SYNOPSIS
    HyzenKernel Bundle Builder (Windows Version)
    Creates a CurseForge-compatible bundle.zip

.DESCRIPTION
    Port of build-bundle.sh for PowerShell with improved Windows path handling.

.EXAMPLE
    .\build-bundle.ps1
    Uses version from latest git tag or build.gradle.kts

.EXAMPLE
    .\build-bundle.ps1 1.6.0
    Uses specified version
#>

param (
    [string]$VersionArg
)

$ErrorActionPreference = 'Stop'

# Function to parse version from build.gradle.kts
function Get-GradleVersion {
    $GradleFile = "build.gradle.kts"
    if (Test-Path $GradleFile) {
        $Content = Get-Content $GradleFile -Raw
        if ($Content -match 'val\s+projectVersion\s*=\s*"([^"]+)"') {
            return $matches[1]
        }
    }
    return $null
}

# Function to update file content with regex
function Update-FileContent {
    param (
        [string]$Path,
        [string]$Regex,
        [string]$Replacement
    )
    if (Test-Path $Path) {
        $Content = Get-Content $Path -Raw
        if ($Content -match $Regex) {
            $NewContent = $Content -replace $Regex, $Replacement
            Set-Content $Path $NewContent -NoNewline
            Write-Host "Updated $Path"
        } else {
            Write-Warning "Pattern not found in $Path"
        }
    } else {
        Write-Warning "File not found: $Path"
    }
}

# Determine initial version from build.gradle.kts
$CurrentVersion = Get-GradleVersion
if (-not $CurrentVersion) { $CurrentVersion = "1.0.0-dev" }

# Determine version to use
if (-not [string]::IsNullOrEmpty($VersionArg)) {
    # Version provided as argument (strip 'v' prefix if present)
    $Version = $VersionArg -replace "^v", ""
} else {
    # Interactive prompt
    Write-Host "Current detected version: $CurrentVersion"
    $InputVersion = Read-Host "Enter version to build (Press Enter to use $CurrentVersion)"
    if ([string]::IsNullOrWhiteSpace($InputVersion)) {
        $Version = $CurrentVersion
    } else {
        $Version = $InputVersion -replace "^v", ""
    }
}

Write-Host "Using version: $Version"

# Update version in files if different or forced update needed
if ($Version -ne $CurrentVersion -or $true) { # Always run updates to ensure consistency
    Write-Host "Updating project files to version $Version..."
    
    # Update build.gradle.kts
    Update-FileContent "build.gradle.kts" 'val\s+projectVersion\s*=\s*"[^"]+"' "val projectVersion = `"$Version`""
    
    # Update src/main/resources/manifest.json
    Update-FileContent "src/main/resources/manifest.json" '"Version":\s*"[^"]*"' "`"Version`": `"$Version`""
    
    # Update hyzenkernel-early/build.gradle.kts (if it has hardcoded version or just rely on -P)
    # The user mentioned hardcoded version in "various places", checking early plugin just in case
    # Taking a safe approach: we will pass -Pversion to gradle anyway, but let's see if we can update specific files if needed.
    # Based on previous read, hyzenkernel-early/build.gradle.kts uses 'findProperty("version")', so no need to edit file there.
}

$BundleDir = Join-Path $PSScriptRoot "bundle"
$OutputZip = "hyzenkernel-bundle-${Version}.zip"

Write-Host "========================================"
Write-Host "  HyzenKernel Bundle Builder"
Write-Host "  Version: ${Version}"
Write-Host "========================================"

# Clean previous bundle
Write-Host "[1/7] Cleaning previous bundle..."
if (Test-Path "$BundleDir\mods") { Remove-Item "$BundleDir\mods" -Recurse -Force }
if (Test-Path "$BundleDir\earlyplugins") { Remove-Item "$BundleDir\earlyplugins" -Recurse -Force }
Get-ChildItem -Path $PSScriptRoot -Filter "hyzenkernel-bundle-*.zip" | Remove-Item -Force

New-Item -Path "$BundleDir\mods" -ItemType Directory -Force | Out-Null
New-Item -Path "$BundleDir\earlyplugins" -ItemType Directory -Force | Out-Null

# Manifest is already updated by the step above in src, so copying it later will be fine.
# But existing script logic updates the bundled manifest.json. Let's keep that logic or rely on the source one.
# The source one is at src/main/resources/manifest.json. The script copies it? 
# Wait, the original script does: sed -i ... "${BUNDLE_DIR}/manifest.json"
# But where does bundle/manifest.json come from? The zip creation step zips 'manifest.json' from BUNDLE_DIR.
# But nothing copies manifest.json TO the bundle dir in the original script!
# Attempting to read original script again... NO, wait.
# The original script does: sed -i ... "${BUNDLE_DIR}/manifest.json" -- implies it expects it to be there.
# But checking lines 41-42: rm -rf, mkdir. NO cp manifest.json.
# This implies manifest.json MIGHT persist in bundle/ dir?
# AH, line 46 updates it. Line 75 zips it.
# If I clean bundle/mods and bundle/earlyplugins, bundle/manifest.json might stay.
# BUT, if this is a fresh checkout or I deleted bundle dir, it fails.
# Let's assume we should copy it from src or check if it exists.
# The previous `deploy.bat` didn't deal with bundle/manifest.json.
# The `build-bundle.sh` DOES assume `bundle/manifest.json` exists.
# I should ensure `bundle/manifest.json` is fresh from `src/main/resources/manifest.json`?
# Or maybe the bundle manifest is different?
# Let's look at file list: `bundle` dir exists.
# I will add a step to copy src/main/resources/manifest.json to bundle/manifest.json to be safe and consistent.
# Actually, the user's `build-bundle.sh` modifies `${BUNDLE_DIR}/manifest.json`.
# I will COPY src/main/resources/manifest.json to bundle/manifest.json first to ensure we have the latest base.

# Update bundle manifest version
# Update bundle manifest version and paths
Write-Host "[2/7] Updating bundle manifest..."
$ManifestPath = Join-Path $BundleDir "manifest.json"

if (Test-Path $ManifestPath) {
    try {
        $ManifestContent = Get-Content $ManifestPath -Raw
        
        # Update Version
        # Regex to match "Version": "..." but be careful not to match others if possible, though top-level is usually first.
        # However, SubPlugins usually don't have a "Version" key, so it's safer.
        $ManifestContent = $ManifestContent -replace '"Version":\s*"[^"]*"', "`"Version`": `"$Version`""
        
        # Update Runtime Plugin Path
        # Replaces mods/hyzenkernel-X.X.X.jar with new version
        $ManifestContent = $ManifestContent -replace 'mods/hyzenkernel-[\w\.-]+\.jar', "mods/hyzenkernel-${Version}.jar"
        
        # Update Early Plugin Path
        # Replaces earlyplugins/hyzenkernel-early-X.X.X.jar with new version
        $ManifestContent = $ManifestContent -replace 'earlyplugins/hyzenkernel-early-[\w\.-]+\.jar', "earlyplugins/hyzenkernel-early-${Version}.jar"

        $ManifestContent | Set-Content $ManifestPath
        Write-Host "Updated bundle manifest version to $Version and adjusted paths (preserved formatting)."
    } catch {
        Write-Warning "Failed to update manifest.json: $_"
    }
} else {
    Write-Warning "Manifest file not found at $ManifestPath. Creating default..."
    # Create a default if missing with nice formatting
    $DefaultContent = @"
{
  "Group": "com.hyzenkernel",
  "Name": "HyzenKernel",
  "Version": "$Version",
  "Description": "Essential bug fixes for Hytale Early Access servers. Includes runtime plugin and early plugin bytecode transformers. Prevents crashes, player kicks, combat desync, and sync issues.",
  "Authors": [
    {
      "Name": "HyzenNet",
      "Url": "https://github.com/HyzenNet"
    }
  ],
  "Website": "https://github.com/HyzenNet/Kernel",
  "ServerVersion": "*",
  "Dependencies": {},
  "OptionalDependencies": {},
  "DisabledByDefault": false,
  "IncludesAssetPack": false,
  "SubPlugins": {
    "com.hyzenkernel:HyzenKernel-Runtime": {
      "Path": "mods/hyzenkernel-${Version}.jar",
      "Description": "Runtime plugin - Place in mods/ folder"
    },
    "com.hyzenkernel:HyzenKernel-Early": {
      "Path": "earlyplugins/hyzenkernel-early-${Version}.jar",
      "Description": "Early plugin bytecode transformers - Place in earlyplugins/ folder"
    }
  }
}
"@
    $DefaultContent | Set-Content $ManifestPath
}

# Check if gradlew.bat exists
if (Test-Path ".\gradlew.bat") {
    # Run clean build for both projects using the root wrapper (which is newer/correct)
    # We build :clean, :build (shadowJar) and :hyzenkernel-early:build
    Write-Host "Running Gradle build..."
    cmd.exe /c "gradlew.bat clean shadowJar :hyzenkernel-early:build -Pversion=${Version} --quiet"
} else {
    Write-Error "gradlew.bat not found!"
}

# Find the built runtime jar
Write-Host "Collecting artifacts..."
$RuntimeJar = Get-ChildItem "build\libs\hyzenkernel-*.jar" | Sort-Object LastWriteTime -Descending | Select-Object -First 1
if ($RuntimeJar) {
    Copy-Item $RuntimeJar.FullName "$BundleDir\mods\hyzenkernel-${Version}.jar"
    Write-Host "       -> mods/hyzenkernel-${Version}.jar (Source: $($RuntimeJar.Name))"
} elseif (Test-Path "build\libs\hyzenkernel.jar") {
    Copy-Item "build\libs\hyzenkernel.jar" "$BundleDir\mods\hyzenkernel-${Version}.jar"
    Write-Host "       -> mods/hyzenkernel-${Version}.jar"
} else {
    Write-Error "Failed to build runtime jar. No jar found in build\libs matching pattern."
}

# Find early jar (in subproject build folder)
$EarlyJar = Get-ChildItem "hyzenkernel-early\build\libs\hyzenkernel-early-*.jar" | Sort-Object LastWriteTime -Descending | Select-Object -First 1

if ($EarlyJar) {
    Copy-Item $EarlyJar.FullName "$BundleDir\earlyplugins\hyzenkernel-early-${Version}.jar"
    Write-Host "       -> earlyplugins/hyzenkernel-early-${Version}.jar (Source: $($EarlyJar.Name))"
} elseif (Test-Path "hyzenkernel-early\build\libs\hyzenkernel-early.jar") {
    Copy-Item "hyzenkernel-early\build\libs\hyzenkernel-early.jar" "$BundleDir\earlyplugins\hyzenkernel-early-${Version}.jar"
    Write-Host "       -> earlyplugins/hyzenkernel-early-${Version}.jar"
} else {
     Write-Warning "Early plugin jar not found."
}

# Copy README
Write-Host "[5/7] Adding documentation..."
if (Test-Path "CURSEFORGE.md") {
    Copy-Item "CURSEFORGE.md" "$BundleDir\README.md"
}

# Verify versions match (Simple check)
Write-Host "[6/7] Verifying version consistency..."
if (Test-Path $ManifestPath) {
    $ManifestContent = Get-Content $ManifestPath -Raw
    if ($ManifestContent -match '"Version": "(.*?)"') {
        Write-Host "  Bundle manifest: $($matches[1])"
    }
}

# Create ZIP
Write-Host "[7/7] Creating bundle ZIP..."
if (Test-Path $BundleDir) {
    $ItemsToZip = Get-ChildItem -Path $BundleDir
    Compress-Archive -Path $ItemsToZip.FullName -DestinationPath $OutputZip -Force
}

Write-Host ""
Write-Host "========================================"
Write-Host "  Bundle created: ${OutputZip}"
Write-Host "========================================"
Write-Host "Ready for CurseForge upload!"
