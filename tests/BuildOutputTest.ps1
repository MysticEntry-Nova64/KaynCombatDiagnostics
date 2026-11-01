param(
 [Parameter(Mandatory=$true)][string]$GameRoot,
 [Parameter(Mandatory=$true)][string]$JdkRoot
)
$ErrorActionPreference='Stop'
# Keep all fixtures and generated outputs under build; never touch the game installation.
$repo=Split-Path $PSScriptRoot -Parent
$fixture=Join-Path $repo ("build/build-regression-"+[Guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Path $fixture | Out-Null
Copy-Item -LiteralPath (Join-Path $repo 'build.ps1') -Destination $fixture
Copy-Item -LiteralPath (Join-Path $repo 'src'),(Join-Path $repo 'tests') -Destination $fixture -Recurse
$output=Join-Path $fixture 'custom-output'
New-Item -ItemType Directory -Path $output | Out-Null
$sentinel=Join-Path $output 'keep.txt'
Set-Content -LiteralPath $sentinel -Value 'unrelated output must survive'
$removedSource=Join-Path $fixture 'src/kayn/diagnostics/RemovedAfterFirstBuild.java'
Set-Content -LiteralPath $removedSource -Value 'package kayn.diagnostics; public class RemovedAfterFirstBuild {}'
& (Join-Path $fixture 'build.ps1') -GameRoot $GameRoot -JdkRoot $JdkRoot -BuildRoot $output
if(-not $?){throw 'First fixture build failed'}
$jar=Join-Path $fixture 'jars/kayn-combat-diagnostics.jar'
$entries=& "$JdkRoot/bin/jar.exe" --list --file $jar
if($LASTEXITCODE -ne 0){throw 'Cannot inspect first JAR'}
if($entries -notcontains 'kayn/diagnostics/RemovedAfterFirstBuild.class'){throw 'Fixture class was not compiled'}
# This exact source was created above inside the isolated fixture.
Remove-Item -LiteralPath $removedSource
& (Join-Path $fixture 'build.ps1') -GameRoot $GameRoot -JdkRoot $JdkRoot -BuildRoot $output
if(-not $?){throw 'Second fixture build failed'}
$entries=& "$JdkRoot/bin/jar.exe" --list --file $jar
if($LASTEXITCODE -ne 0){throw 'Cannot inspect rebuilt JAR'}
if($entries -contains 'kayn/diagnostics/RemovedAfterFirstBuild.class'){throw 'Rebuild packaged a deleted source class'}
if($entries -match 'Test\.class$'){throw 'Production JAR contains test classes'}
if((Get-Content -LiteralPath $sentinel -Raw).Trim() -ne 'unrelated output must survive'){throw 'Build changed unrelated files'}
Write-Output 'PASS: build output regression (deleted class excluded, tests excluded, unrelated file preserved)'
