# Detailed usage and limitations

Version 1.5.0 adds a compact rounded console board, removes the external font-library dependency and makes the Ctrl shortcuts configurable. Coloured restrictions, freshness indicators and five-projectile histories remain separate from AI behaviour.

Standalone, optional Starsector 0.98a-RC8 combat observer. No external mod or library dependencies; uses only the installed game's API, bundled OpenGL runtime and font atlas. Does not require Verdant, Console Commands, a hullmod, ship IDs or per-ship setup. Enable the mod in the launcher once.

Its purpose is to check weapons on custom ship designs: observed firing, projectile/impact counts, firing time, and evidence about readiness or obstruction. A clear line or readiness estimate does not guarantee the game's AI will fire, or that a future projectile will hit. Damage attribution is not a design-validation goal; legacy damage columns remain in the file format for compatibility and are not a validated per-weapon metric.

- Ctrl+F7: show/hide help, including recording and evidence explanations.
- Ctrl+F8: start/stop diagnostics. Starts off in each battle. Starts a fresh measurement when enabled.
- Ctrl+F9: pin your selected ship target; press again to follow your controlled ship. Select a friendly or enemy ship using the normal game targeting controls. With no target, stays on your own ship.
- Ctrl+F10: next weapon page. Six weapons per page; all weapons are included in the saved readout regardless of page.

Automatically follows changes to the controlled ship in self mode. Switching subjects closes the previous measurement and resets counters. Target mode stays pinned until toggled back. Counters cover only the enabled observation interval. Decorative weapons are skipped. Modules/fighters are separate ships: target or control the specific entity to inspect it; this does not aggregate parent and module weapons.

The panel shows hull, flux, recording status, FIRING activity, group/control mode,
target provenance, projectile/impact counts and beam duration/hit frames.
All observed restrictions appear together with independent colours:
Blocked (orange), Distance (yellow), Fluxing (cyan), NoTarget (grey), Damaged (red),
Cooldown (blue), NoAmmo (red), Hold (purple), Arc (amber), Overload (red), Unknown
(grey), and AIHold (purple, explicit individual autofire request is false).
Hold is the forced-no-fire flag; neither Hold nor AIHold reveals the AI's private
reason. Fluxing means venting or insufficient reported flux headroom. NoTarget
means no target is known to this observer, including manual control. It appears
once as a coloured restriction; a second white NO_TARGET label is not displayed.
Restriction labels stay visible even while FIRING is independently observed.
The TSV appends a `restrictions` column; existing priority `state` remains for
compatibility. HUD labels and the TSV column are captured in the same refresh.

Each weapon retains five short (90 world-unit) initial observed projectile
direction lines: bright green, green, dark green, very dark green, black.
The sixth evicts the oldest. Lines are detached world coordinates: they stay at
the projectile's reported spawn point as the ship moves. Each newly observed
projectile counts separately, including simultaneous pellets and burst rounds;
this is not a trigger counter. Missing/invalid spawn or facing data produces no
line. Already airborne projectiles at session start are excluded. Missiles show
first-observed facing, not a predicted trajectory. Continuous beams have their
duration/contact readout rather than fabricated projectile shots. Coincident
lines overlap; newest draws last. Black may be hard to see against dark space.

The compact title combines COMBAT DIAGNOSTICS, observed simulation time and
Recording (or an explicit recording warning). Ship identity follows, then hull,
flux and weapon count on one line. Page numbering appears only for multiple pages.
Detailed shortcuts and archive explanations are in Ctrl+F7 help.

The board sizes itself to the visible content, with rounded corners, a muted teal
border and bounded scrolling. Font parsing, layout, restriction interpretation,
shot history and recording are separate modules. It never edits game classes,
ship models, weapon data, AI decisions or shared SpriteAPI properties. It adds
only its own observer/listeners, readout files and drawing; listeners are removed
when observation ends. Rendering saves and restores its OpenGL state.

To resolve shortcut collisions, edit this mod's `data/config/settings.json`:
`kayn_diagnostics_toggle_key`, `subject_key`, `page_key`, `help_key`, `up_key` and
`down_key` (each with the `kayn_diagnostics_` prefix). Defaults are F8, F9, F10,
F7, UP and DOWN; every shortcut requires Ctrl. Allowed values are F1–F12, UP
and DOWN, with no duplicates. Invalid configuration is reported and defaults
are retained. Restart the game after editing. These safeguards reduce conflict
risk; they do not establish compatibility with every possible mod.

Freshness displays LIVE, PAUSED, FINAL / STALE or STALE / FAILED, plus age
in simulation seconds since the last complete snapshot. Normal refresh is 0.25s;
an age above 0.5s is stale. Pause freezes collection and history, and cached
FIRING must be interpreted with the stale banner. Ctrl+Up/Down scrolls the panel;
text is clipped to its bounds. Ctrl+F10 changes the six-weapon page and resets scroll.

Geometry rejects non-finite muzzle/aim/range and invalid entity/shield geometry
as UNKNOWN rather than CLEAR. Nonpositive range and zero-length aim are UNKNOWN.
Segments are closed: endpoint touches and collinear overlap count as contact;
the nearest overlap point is used. The existing small numeric tolerances remain
local geometry tolerances, not a guarantee for arbitrarily scaled inputs.
Shield boundary intersections still respect the active arc.

Targets come from an active, unselected autofire group, an AI-controlled selected group (including player autopilot) with an explicit ship-AI target and aim point, or a beam currently applying damage. The nearest enemy and a manually selected ship target are never substituted as weapon intent. Ship-AI-selected intent is distinguished as SHIP_AI_SELECTED_TARGET in the TSV; it is not a guarantee that every weapon in that group can fire. The HUD separates TARGET/NO_TARGET from firing activity. The TSV retains NONE/NO_TARGET when an active autofire plugin reports no target, and UNOBSERVED/TARGET_UNKNOWN when weapon intent is unavailable (including manual control). Actual firing remains FIRING even without target evidence. A live beam contact can establish FIRING and its contact target independently of autofire. Range uses the observed contact point, or a collision-radius approximation for an autofire entity; it is not a guarantee of projectile reach. `AI_HOLD` describes the individual autofire request. `READY_OBSERVED` means the measured readiness checks passed; it does not promise that the weapon will fire or hit.

Recording status is visible in the panel. A failed live-file write is reported once and stops further live-file attempts for that measurement; collection and the in-memory readout continue. Finishing attempts a separate archive even after live-file failure. Archive failures identify their filename in the log and combat message area. The queue retains the exact failed report across subject changes and retries one report every five unpaused simulation seconds, including while diagnostics are disabled and after battle completion if the engine still advances this plugin. Successful writes remove only that report; other unresolved failures keep the warning visible.

The queue holds at most eight reports and 524,288 UTF-16 characters in total (up to 1 MiB of character payload, plus bounded entry overhead). Each report gets its initial write and four automatic retries. Exhausted reports remain in memory; explicitly enabling diagnostics with Ctrl+F8 grants exhausted entries another five retry attempts. Queue overflow or an oversized failed report is reported explicitly and does not evict older reports. An unqueued-loss warning remains for the rest of combat. This is an in-memory combat-local queue: pending reports do not survive the plugin being discarded or the game closing.

Snapshot/readout and observation runtime exceptions stop that measurement, report the failure, detach listeners on a best-effort basis and archive the last complete report with an explicit snapshotStale marker. TSV, per-slot readouts and HUD construction publish only after a complete snapshot succeeds. If the first snapshot fails, the fallback explicitly says noCompleteSnapshot instead of reusing another ship's report. Ctrl+F8 can start a fresh measurement. Runtime exceptions from diagnostic input and rendering are also contained; fatal JVM Errors are not swallowed. A broken host listener-removal API cannot be forced to remove a listener, but cleanup still attempts the remaining removals.

No steering, weapon or stat changes. Collection and retry timing pause with combat. Disabling stops scans and live writes and hides the panel; pending archives may still retry. Battle end or subject loss finalizes the measurement. Returning directly out of combat before another plugin update is not guaranteed to produce a final archive; the most recent live snapshot remains.

Automatic local files in Starsector/saves/common:
- kayn_combat_diagnostics_live.tsv.data: latest snapshot, approximately once per simulation second.
- kayn_combat_diagnostics_<session UUID>.tsv.data: completed measurements on disable, subject change, death or battle end.

Archive files are retained so switching ships does not discard completed results. They contain no campaign save changes. Disable the mod in the launcher and restart to remove its combat plugin entirely.

Limits: firing line is a straight geometric estimate against intervening ships/shields/asteroids, excluding the firing hull and intended target. It is not the game's AI collision predicate and does not model turret artwork, projectile width/lead, missile paths or future motion. Range uses collision-radius approximation. Raw damage may include overkill and hulks; damage without a weapon identity remains unassigned. Very brief events may occur between snapshots. Generic/modded weapon scripts and unusual multi-entity ships need individual validation.

Verdant compatibility: prototype 0.3.5 suppresses its embedded overlay when this mod is enabled, avoiding duplicate observers/panels. Earlier prototypes may show both panels.

Java source and test source included. Build instructions and actual test evidence are in BUILD.md and TEST-RESULTS.md. No proprietary ship art, game classes or fonts are bundled; the installed game's font atlas provides text rendering.

## Installation packages and checks

For a binary release, extract the `KaynCombatDiagnostics` folder from the release ZIP
into `Starsector/mods`, then enable the mod in the launcher.
The GitHub source download needs a local build first. See BUILD.md for building and
packaging, and RELEASE-NOTES.md for the 1.5.0 release summary.

GitHub checks validate documentation and 50 dependency-free math/state, geometry, font and archive-queue assertions.
The full local game-API fixture suite and binary packaging require a local Starsector installation.

## License

Project code is available under the MIT license; see LICENSE. Starsector and its bundled runtimes remain separately licensed dependencies and are not bundled.
