# Kayn Combat Diagnostics 1.0.0

Standalone, optional Starsector 0.98a-RC8 combat observer. Requires LazyLib; does not require Verdant, Console Commands, a hullmod, ship IDs or per-ship setup. Enable the mod in the launcher once.

- Ctrl+F8: start/stop diagnostics. Starts off in each battle. Starts a fresh measurement when enabled.
- Ctrl+F9: pin your selected ship target; press again to follow your controlled ship. Select a friendly or enemy ship using the normal game targeting controls. With no target, stays on your own ship.
- Ctrl+F10: next weapon page. Six weapons per page; all weapons are included in the saved readout regardless of page.

Automatically follows changes to the controlled ship in self mode. Switching subjects closes the previous measurement and resets counters. Target mode stays pinned until toggled back. Counters cover only the enabled observation interval. Decorative weapons are skipped. Modules/fighters are separate ships: target or control the specific entity to inspect it; this does not aggregate parent and module weapons.

The panel shows hull, flux, raw outgoing shield/armor/hull damage, actual FIRING status, weapon group number, SELECTED/AUTO/MANUAL control mode, individual AI firing request, approximate range MET/OUT, geometry CLEAR/BLOCKED, projectiles/impacts, and beam firing time. Blocked rows are red. FIRING is observed independently of the individual autofire AI. The detailed file retains readyObserved as an individual-AI readiness estimate, not a group-fire permission flag.

No steering, weapon or stat changes. Collection pauses with combat. Disabling detaches listeners, stops scans/writes and hides the panel. Battle end or subject loss finalizes the measurement. Returning directly out of combat before another plugin update is not guaranteed to produce a final archive; the most recent live snapshot remains.

Automatic local files in Starsector/saves/common:
- kayn_combat_diagnostics_live.tsv.data: latest snapshot, approximately once per simulation second.
- kayn_combat_diagnostics_<session UUID>.tsv.data: completed measurements on disable, subject change, death or battle end.

Archive files are retained so switching ships does not discard completed results. They contain no campaign save changes. Disable the mod in the launcher and restart to remove its combat plugin entirely.

Limits: firing line is a straight geometric estimate against intervening ships/shields/asteroids, excluding the firing hull and intended target. It is not the game's AI collision predicate and does not model turret artwork, projectile width/lead, missile paths or future motion. Range uses collision-radius approximation. Raw damage may include overkill and hulks; damage without a weapon identity remains unassigned. Very brief events may occur between snapshots. Generic/modded weapon scripts and unusual multi-entity ships need individual validation.

Verdant compatibility: prototype 0.3.5 suppresses its embedded overlay when this mod is enabled, avoiding duplicate observers/panels. Earlier prototypes may show both panels.

Java source and test source included. Build instructions and actual test evidence are in BUILD.md and TEST-RESULTS.md. No proprietary ship art, game classes or fonts are bundled; LazyLib and the game's installed font provide text rendering.

## License

Project code is available under the MIT license; see LICENSE. Starsector and LazyLib remain separately licensed dependencies and are not bundled.
