# 1.5.0 — standalone rendering and compact board

Removes the LazyLib dependency. Font metrics, board layout and GL rendering have
separate ownership; uses the installed game font without bundling or editing it.
Adds a small content-sized rounded board, configurable unique Ctrl shortcuts,
explicit footer freshness states, and double intermediates for geometry to avoid
float-overflow false-clear results. Preserves all coloured restrictions and the
five-projectile history from 1.4.0 and compact header/help from 1.4.1.

The final local game run passed the exercised controls and multi-ship checks;
333 local assertions and 50 portable assertions pass. The final combat interval
contains zero ERROR/WARN entries. Existing unrelated startup errors remain.
Includes 24 GIF demonstrations, with live footage and display fixtures separated.
No game classes, ship models, weapon data, AI, stats or other mods are patched.
See TEST-RESULTS.md for evidence scope and limitations.

# 1.4.1 — compact diagnostic HUD

Combines title, observed simulation timer and recording state on one line;
groups hull/flux/weapon count, hides single-page numbering and moves detailed
controls/archive explanations into Ctrl+F7 help. Removes duplicate white NO_TARGET;
keeps each coloured restriction once and firing activity separate. Retains
selected/controlled ship support, explicit failure warnings and paused evidence.
No AI, weapon, targeting or combat-state behaviour changes.

# 1.4.0 — weapon evidence and projectile histories

Adds simultaneous independently coloured restrictions, target provenance,
paused/stale/final evidence status and scrollable/clipped HUD text. Five detached
projectile-direction lines per weapon age from bright green to black; the sixth
removes the oldest. Counts observed projectiles, not trigger pulls; continuous
beams retain their duration/contact display. Invalid geometry becomes UNKNOWN;
closed-segment contact includes collinear overlap and touching endpoints.

Remains an opt-in observer for the controlled or pinned selected ship. No firing,
targeting, steering, AI or stat mutation. No optional comparison, Shipwright,
damage-attribution research, durable archive spool or performance refactor added.
Existing snapshot, lifecycle and archive protections remain. See TEST-RESULTS.md
for this candidate's executed verification and limits.

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
