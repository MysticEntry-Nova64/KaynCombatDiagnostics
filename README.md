# Kayn Combat Diagnostics 1.3.0

Standalone, optional Starsector 0.98a-RC8 combat observer. Requires LazyLib; does not require Verdant, Console Commands, a hullmod, ship IDs or per-ship setup. Enable the mod in the launcher once.

Its purpose is to check weapons on custom ship designs: observed firing, projectile/impact counts, firing time, and evidence about readiness or obstruction. A clear line or readiness estimate does not guarantee the game's AI will fire, or that a future projectile will hit. Damage attribution is not a design-validation goal; legacy damage columns remain in the file format for compatibility and are not a validated per-weapon metric.

- Ctrl+F8: start/stop diagnostics. Starts off in each battle. Starts a fresh measurement when enabled.
- Ctrl+F9: pin your selected ship target; press again to follow your controlled ship. Select a friendly or enemy ship using the normal game targeting controls. With no target, stays on your own ship.
- Ctrl+F10: next weapon page. Six weapons per page; all weapons are included in the saved readout regardless of page.

Automatically follows changes to the controlled ship in self mode. Switching subjects closes the previous measurement and resets counters. Target mode stays pinned until toggled back. Counters cover only the enabled observation interval. Decorative weapons are skipped. Modules/fighters are separate ships: target or control the specific entity to inspect it; this does not aggregate parent and module weapons.

The panel shows hull, flux, recording status, actual FIRING status, weapon group number, SELECTED/AUTO/MANUAL control mode, individual AI firing request, approximate range MET/OUT, geometry CLEAR/BLOCKED, projectiles/impacts, and beam firing time with observed hit frames. Weapon text is white by default. The panel shows NO_TARGET when no target is observed, and TARGET SHIP/MISSILE/PROJECTILE/POINT when target evidence is available. Unknown range and line show ?. An observed out-of-range target makes the entry yellow; venting that blocks firing makes it orange; an observed firing-line obstruction makes it red. Red takes priority over orange, then yellow, then white. Beam hit frames count damage-bearing updates, not separate shots. FIRING is observed independently of the individual autofire AI. The detailed file retains readyObserved as an individual-AI readiness estimate, not a group-fire permission flag.

Known observations take priority over an apparent ready state: `COOLDOWN`, `FORCED_NO_FIRE`, `NO_AMMO` and `FLUX_HEADROOM_LOW` identify current measured restrictions. Disabled weapons and ship-level flux restrictions retain their existing priority, and actual `FIRING` remains independent of those readiness estimates. More than one restriction may exist; the TSV retains the separate flags and values. `FLUX_HEADROOM_LOW` compares the reported firing cost with current flux headroom; unusual scripts and beam behavior still need individual validation.

Targets come from an active, unselected autofire group, an AI-controlled selected group (including player autopilot) with an explicit ship-AI target and aim point, or a beam currently applying damage. The nearest enemy and a manually selected ship target are never substituted as weapon intent. Ship-AI-selected intent is distinguished as SHIP_AI_SELECTED_TARGET in the TSV; it is not a guarantee that every weapon in that group can fire. The HUD separates TARGET/NO_TARGET from firing activity. The TSV retains NONE/NO_TARGET when an active autofire plugin reports no target, and UNOBSERVED/TARGET_UNKNOWN when weapon intent is unavailable (including manual control). Actual firing remains FIRING even without target evidence. A live beam contact can establish FIRING and its contact target independently of autofire. Range uses the observed contact point, or a collision-radius approximation for an autofire entity; it is not a guarantee of projectile reach. `AI_HOLD` describes the individual autofire request. `READY_OBSERVED` means the measured readiness checks passed; it does not promise that the weapon will fire or hit.

Recording status is visible in the panel. A failed live-file write is reported once and stops further live-file attempts for that measurement; collection and the in-memory readout continue. Finishing still attempts a separate archive. Any archive failure is logged with its filename and reported through the combat message area, even when disabling hides the overlay. Starting a fresh measurement retries live recording; a previous archive warning stays visible until an archive succeeds. Failed archives are not queued or automatically retried: the current in-memory snapshot remains available until another measurement replaces it.

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
