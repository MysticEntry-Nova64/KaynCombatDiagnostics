$ErrorActionPreference = 'Stop'
$repo = Split-Path $PSScriptRoot -Parent
$output = Join-Path $repo ('build/portable-' + [Guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Force $output | Out-Null
# These are the only production/test sources independent of proprietary game APIs.
& javac --release 17 -d $output (Join-Path $repo 'src/kayn/diagnostics/DiagnosticMath.java') (Join-Path $PSScriptRoot 'DiagnosticMathTest.java')
if ($LASTEXITCODE -ne 0) { throw 'Portable compilation failed' }
& java -cp $output kayn.diagnostics.DiagnosticMathTest
if ($LASTEXITCODE -ne 0) { throw 'Portable tests failed' }
