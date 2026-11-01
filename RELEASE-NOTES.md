# Kayn Combat Diagnostics 1.3.1

Optional combat observer for Starsector 0.98a-RC8. Requires LazyLib and Java 17 or newer.

## Changes since 1.3.0

- Separate target evidence from weapon readiness/state priority.
- Contain snapshot runtime exceptions, detach owned listeners, preserve explicitly stale last-complete reports and permit restart.
- Retain immutable failed archives in a bounded queue, retry on unpaused simulation time and show pending/exhausted warnings.
- Verify completed archive contents by readback: the game can log write failures without throwing.
- Add archive queue CI coverage and expand the local suite to 222 assertions.

Live verification used Verdant, Lasher and Hammerhead on Starsector 0.98a-RC8 / Java 28-beta. Injected snapshot failure was contained; a locked archive recovered exactly after unlocking and switching ships. The final combat interval had four expected injected errors and no unexpected warnings/errors. The helper checked recovery too early twice; independent later file comparison verified recovery. See TEST-RESULTS.md for scope and limitations.

The queue is memory-only, bounded to eight reports and 524,288 UTF-16 characters. Automatic retries are limited; exhausted reports require explicit rearming. Pending reports are lost at process exit or plugin disposal. This release makes no broad performance or all-mod compatibility claim.

## Installation

Extract the ZIP's `KaynCombatDiagnostics` folder into `Starsector/mods`.
Confirm `Starsector/mods/KaynCombatDiagnostics/mod_info.json` and its `jars/`
directory exist, install LazyLib separately, then enable the mod in the launcher.
Avoid nesting a second `KaynCombatDiagnostics` folder inside the mod directory.
Ctrl+F8 starts/stops diagnostics; Ctrl+F9 switches target/self; Ctrl+F10 pages weapons.

The adjacent `.zip.sha256` file contains the release ZIP's SHA-256 hash.
Compare it with `Get-FileHash ./KaynCombatDiagnostics-1.3.0.zip -Algorithm SHA256`.


## Provenance

The v1.3.1 tag identifies this updated source and binary release. The original v1.3.0 tag and assets are preserved. Proprietary game dependencies and the temporary test helper are excluded from the package.
