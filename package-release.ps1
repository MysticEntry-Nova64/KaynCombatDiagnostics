param(
    [Parameter(Mandatory=$true)][string]$GameRoot,
    [Parameter(Mandatory=$true)][string]$JdkRoot,
    [string]$OutputRoot = "$PSScriptRoot/dist"
)
$ErrorActionPreference = 'Stop'
$manifest = Get-Content -LiteralPath "$PSScriptRoot/mod_info.json" -Raw | ConvertFrom-Json
$version = [string]$manifest.version
if ($version -notmatch '^\d+\.\d+\.\d+$') { throw 'Expected a numeric release version' }
New-Item -ItemType Directory -Force $OutputRoot | Out-Null
$zip = Join-Path $OutputRoot "KaynCombatDiagnostics-$version.zip"
$checksum = "$zip.sha256"
if ((Test-Path -LiteralPath $zip) -or (Test-Path -LiteralPath $checksum)) {
    throw 'Release output already exists; choose a new OutputRoot to preserve it'
}
# Build and run the complete game-API fixture suite before packaging.
& "$PSScriptRoot/build.ps1" -GameRoot $GameRoot -JdkRoot $JdkRoot
$staging = Join-Path $PSScriptRoot ('build/release-' + [Guid]::NewGuid().ToString('N'))
$mod = Join-Path $staging 'KaynCombatDiagnostics'
New-Item -ItemType Directory -Force "$mod/data/config", "$mod/jars" | Out-Null
foreach ($file in @('mod_info.json', 'README.md', 'BUILD.md', 'TEST-RESULTS.md', 'LICENSE', 'RELEASE-NOTES.md')) {
    Copy-Item -LiteralPath (Join-Path $PSScriptRoot $file) -Destination $mod
}
Copy-Item -LiteralPath "$PSScriptRoot/data/config/settings.json" -Destination "$mod/data/config"
Copy-Item -LiteralPath "$PSScriptRoot/jars/kayn-combat-diagnostics.jar" -Destination "$mod/jars"
Add-Type -AssemblyName System.IO.Compression.FileSystem
[System.IO.Compression.ZipFile]::CreateFromDirectory($staging, $zip)
& "$PSScriptRoot/tests/ReleasePackageTest.ps1" -ZipPath $zip -ExpectedVersion $version
$hash = (Get-FileHash -LiteralPath $zip -Algorithm SHA256).Hash.ToLowerInvariant()
"$hash  $([IO.Path]::GetFileName($zip))" | Set-Content -LiteralPath $checksum -Encoding ascii
Write-Output "Release package: $zip"
Write-Output "SHA-256: $hash"
