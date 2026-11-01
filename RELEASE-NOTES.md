# Kayn Combat Diagnostics 1.3.0

Optional combat observer for Starsector 0.98a-RC8. Requires LazyLib and a
Java 17 or newer game runtime; tested gameplay history uses Miko_Rouge/Java 28.

## Installation

Extract the ZIP's `KaynCombatDiagnostics` folder into `Starsector/mods`.
Confirm `Starsector/mods/KaynCombatDiagnostics/mod_info.json` and its `jars/`
directory exist, install LazyLib separately, then enable the mod in the launcher.
Avoid nesting a second `KaynCombatDiagnostics` folder inside the mod directory.
Ctrl+F8 starts/stops diagnostics; Ctrl+F9 switches target/self; Ctrl+F10 pages weapons.

The adjacent `.zip.sha256` file contains the release ZIP's SHA-256 hash.
Compare it with `Get-FileHash ./KaynCombatDiagnostics-1.3.0.zip -Algorithm SHA256`.

## Changes since 1.0.0

- Observed weapon targets, range, firing activity, projectile/impact counters and
  visible beam firing time/hit frames.
- White default weapon text, yellow range, orange venting and red obstruction,
  with explicit target evidence and no nearest-enemy substitution.
- Fresh measurements and weapon inventories when switching ships.
- Independent live/archive recording failure handling and visible warnings.
- Shield arc crossing, terminal combat lifecycle and stale build output fixes.
- Expanded local API-fixture suite: 178 assertions.

The release preparation corrects the README's HUD description and adds GitHub
documentation/math checks and local binary packaging. Production Java is unchanged.
See TEST-RESULTS.md for revision-specific gameplay evidence and remaining limits.
In particular, geometry/readiness do not guarantee firing or hits, per-weapon damage
attribution is not validated, and failed archives have no automatic retry queue.

Source and build scripts:
[GitHub repository](https://github.com/MysticEntry-Nova64/KaynCombatDiagnostics).
The source download needs building; this binary ZIP already includes the mod JAR.
