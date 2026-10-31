param(
 [Parameter(Mandatory=$true)][string]$GameRoot,
 [Parameter(Mandatory=$true)][string]$JdkRoot,
 [string]$BuildRoot="$PSScriptRoot/build"
)
$ErrorActionPreference='Stop'
$diagClasspath="$GameRoot/starsector-core/*;$GameRoot/mods/LazyLib/jars/*"
$prod=Join-Path $BuildRoot 'production'
$test=Join-Path $BuildRoot 'tests'
New-Item -ItemType Directory -Force $prod,$test,"$PSScriptRoot/jars" | Out-Null
$sources=Get-ChildItem "$PSScriptRoot/src" -Recurse -Filter '*.java' | ForEach-Object FullName
$tests=Get-ChildItem "$PSScriptRoot/tests" -Filter '*.java' | ForEach-Object FullName
& "$JdkRoot/bin/javac.exe" --release 17 -cp $diagClasspath -d $prod $sources
if($LASTEXITCODE -ne 0){throw 'Production compilation failed'}
& "$JdkRoot/bin/javac.exe" --release 17 -cp "$prod;$diagClasspath" -d $test $tests
if($LASTEXITCODE -ne 0){throw 'Test compilation failed'}
foreach($name in @('DiagnosticMathTest','FiringLineTest','FiringLineIntegrationTest','DiagnosticsIntegrationTest')){
 & "$JdkRoot/bin/java.exe" -cp "$test;$prod;$diagClasspath" "kayn.diagnostics.$name"
 if($LASTEXITCODE -ne 0){throw "Failed: $name"}
}
& "$JdkRoot/bin/jar.exe" --create --file "$PSScriptRoot/jars/kayn-combat-diagnostics.jar" -C $prod kayn
if($LASTEXITCODE -ne 0){throw 'Packaging failed'}
