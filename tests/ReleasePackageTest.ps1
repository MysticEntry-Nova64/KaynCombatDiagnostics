param(
    [Parameter(Mandatory=$true)][string]$ZipPath,
    [string]$ExpectedVersion = '1.3.0'
)
$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.IO.Compression.FileSystem
$archive = [IO.Compression.ZipFile]::OpenRead((Resolve-Path -LiteralPath $ZipPath).Path)
try {
    $prefix = 'KaynCombatDiagnostics/'
    $required = @('mod_info.json','data/config/settings.json','jars/kayn-combat-diagnostics.jar',
        'README.md','BUILD.md','TEST-RESULTS.md','LICENSE','RELEASE-NOTES.md') | ForEach-Object { $prefix + $_ }
    $files = @($archive.Entries | Where-Object { -not $_.FullName.EndsWith('/') })
    if ($files.Count -ne $required.Count) { throw 'Unexpected package file count' }
    foreach ($path in $required) {
        $entries = @($files | Where-Object { $_.FullName -ceq $path })
        if ($entries.Count -ne 1 -or $entries[0].Length -eq 0) { throw "Missing, duplicate or empty file: $path" }
    }
    $reader = [IO.StreamReader]::new($archive.GetEntry($prefix + 'mod_info.json').Open())
    try { $manifest = $reader.ReadToEnd() | ConvertFrom-Json } finally { $reader.Dispose() }
    if ($manifest.version -ne $ExpectedVersion -or $manifest.id -ne 'kayn_combat_diagnostics') { throw 'Wrong release identity' }
    if ($manifest.gameVersion -ne '0.98a-RC8' -or @($manifest.dependencies).Count -ne 0) { throw 'Expected supported game metadata and no external dependencies' }
    if (@($manifest.jars).Count -ne 1 -or $manifest.jars[0] -ne 'jars/kayn-combat-diagnostics.jar') { throw 'Wrong runtime JAR path' }
    $reader = [IO.StreamReader]::new($archive.GetEntry($prefix + 'data/config/settings.json').Open())
    try { $settings = $reader.ReadToEnd() | ConvertFrom-Json } finally { $reader.Dispose() }
    if ($settings.plugins.kayn_combat_diagnostics -ne 'kayn.diagnostics.CombatDiagnostics') { throw 'Plugin registration missing' }
    $stream = [IO.MemoryStream]::new()
    try {
        $source = $archive.GetEntry($prefix + $manifest.jars[0]).Open()
        try { $source.CopyTo($stream) } finally { $source.Dispose() }
        $stream.Position = 0
        $jar = [IO.Compression.ZipArchive]::new($stream, [IO.Compression.ZipArchiveMode]::Read, $true)
        try {
            $classes = @($jar.Entries | Where-Object { $_.FullName.EndsWith('.class') })
            if ($classes.Count -eq 0) { throw 'Runtime JAR has no classes' }
            foreach ($entry in $jar.Entries) {
                if ($entry.FullName -ne 'META-INF/' -and $entry.FullName -ne 'META-INF/MANIFEST.MF' -and
                    $entry.FullName -notmatch '^kayn(/diagnostics)?/$' -and
                    $entry.FullName -notmatch '^kayn/diagnostics/[^/]+\.class$') { throw "Unexpected JAR payload: $($entry.FullName)" }
            }
            if ($classes.FullName -match '(Test|MediaLab)[^/]*\.class$') { throw 'Runtime JAR contains test or media fixture classes' }
            if ($classes.FullName -cnotcontains 'kayn/diagnostics/CombatDiagnostics.class') { throw 'Plugin entry class missing' }
            foreach ($entry in $classes) {
                $binary = [IO.BinaryReader]::new($entry.Open())
                try {
                    $header = $binary.ReadBytes(8)
                    if ($header.Length -ne 8 -or [BitConverter]::ToString($header[0..3]) -ne 'CA-FE-BA-BE' -or
                        $header[6] -ne 0 -or $header[7] -ne 61) { throw 'Expected Java 17 class payload' }
                } finally { $binary.Dispose() }
            }
        } finally { $jar.Dispose() }
    } finally { $stream.Dispose() }
    Write-Output "PASS: release $ExpectedVersion layout, documentation, runtime classes and Java 17 compatibility"
} finally { $archive.Dispose() }
