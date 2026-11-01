## v1.3.0 publication verification (2026-11-01)

Release metadata and README now identify version 1.3.0. The source build passed all
178 assertions with JDK 27.0.1.0.3, targeting Java 17. The isolated two-build packaging
regression passed, and the strict documentation build produced a populated search index.
Recording-failure log messages during tests are deliberate injected failures.
No new gameplay session was run for publication; the Java 28 gameplay evidence below
remains tied to the installed JAR and the revision described there.

## Final-build multi-ship and color rerun (2026-11-01, current)

The installed production JAR remained `67621AC9BFF1CD5C5B6CED9AC7FF362BB8D0E163725A450A1FC88CCCB6D9883C`. A fresh build passed all 178 assertions, and all 10 production class payloads match the installed JAR exactly. The game ran through Miko_Rouge on Java 28, with Starsector 0.98a-RC8 FR9.6 visible on the captured frames.

Six actual control transfers among Verdant (23 weapons), Lasher (7) and Hammerhead (8), including return visits, passed all 20 identity, exact inventory and fresh-counter checks. The temporary helper uses setPlayerShipExternal and directly advances the observer on the game thread; it bypasses the normal transfer animation. Counters restart on subject changes rather than retaining cumulative totals per ship.

Fresh native screenshots and matching TSV snapshots confirm white NO_TARGET, yellow observed out-of-range targets, orange venting, and red geometric obstruction. Exact HUD pixel values were checked: white (255,255,255), yellow (255,255,0), orange (255,200,0), red (255,90,90). Lasher's two Light Assault Guns displayed red TARGET SHIP / Range MET / Line BLOCKED when the actual aim ray crossed Hammerhead. Moving the blocker away cleared that obstruction. Returning to Verdant showed red obstruction and yellow range on separate weapons simultaneously. Earlier staged blockers missed the actual aim ray and stayed CLEAR; those captures are retained as failed setups, not red evidence.

These controlled cases mostly mutate real game state while paused, with brief resumed combat intervals. A paused Hammerhead railgun retained its last FIRING flag and stayed white during forced venting, consistent with the production predicate that orange requires venting and not firing. This is not evidence that it fired while venting. Verdant projectile/impact counters increased during resumed combat; independent hit ground truth, all AI intentions, missile/projectile target types and fresh beam contacts were not exhaustively validated in this rerun.

The measured combat log segment contained zero ERROR, WARN or exception mentions. Earlier portions of the installation log contain unrelated mod errors, so this does not certify the entire installation error-free. Stopping diagnostics wrote an ended final archive identical to the final live snapshot. The game was closed, the temporary helper was moved out of the mod directory and the original enabled-mod list was restored byte-for-byte. No production Java changed during this verification pass; nothing was committed or pushed. No performance improvement is claimed.

## Target evidence and color revision (2026-11-01, preceding run)

The current build passes **178 assertions**: 19 math/state, 7 geometry, 17 obstruction/API, 24 lifecycle, 22 recording reliability and 89 weapon diagnostics. Target evidence excludes nearest-enemy substitution and inactive autofire data, distinguishes selected ship AI/player autopilot and current beam contacts, and leaves unavailable range/line unknown. HUD labels separate TARGET/NO_TARGET from firing activity. Priority is red obstruction, orange venting that blocks firing, yellow observed out-of-range, then white.

The final JAR `67621AC9BFF1CD5C5B6CED9AC7FF362BB8D0E163725A450A1FC88CCCB6D9883C` ran in Starsector 0.98a-RC8 FR9.6 via Miko_Rouge/Java 28. Controlled paused scenarios visually confirmed white NO_TARGET, yellow out-of-range TARGET SHIP, white in-range TARGET SHIP, and orange venting. The staged obstruction stayed CLEAR: red is automated-tested but not visually confirmed in this run. Post-baseline logs contained zero ERROR/WARN/exception mentions. This is scoped evidence, not exhaustive mod compatibility or natural-combat validation. Earlier intermediate builds passed 20 ship-transfer identity/inventory/reset checks; they are historical evidence, not a final-build transfer rerun. The temporary mission was removed and original enabled list restored. The final production test build remains installed locally; nothing pushed.

The following sections describe earlier builds and their assertion counts.

## HUD and multi-ship checks (2026-11-01)

The HUD revision passed 139 automated assertions, including NO_TARGET, beam hit-frame display, yellow range, orange venting and preserved red obstruction priority. Test-first failures were observed before implementation. In Starsector 0.98a-RC8 FR9.6 via Miko_Rouge/Java 28, temporary public-API instrumentation transferred actual control six times among Verdant (23 weapons), Lasher (7) and Hammerhead (8), including paused transfers and repeat visits. All 20 live checks passed for ship identity, exact weapon inventories and fresh counters; seven corresponding archives exist. Natural beam contacts displayed 4 and 3 hit frames on-screen, matching the TSV. Yellow range and controlled orange venting were visually checked. The successful combat interval had zero logged errors, warnings or exceptions. Red priority was automated-tested; no new staged obstruction screenshot was taken.

The helper bypassed the normal transfer animation using setPlayerShipExternal and directly advanced the observer on the game thread. Three earlier fixture failures (missing briefing, missing icon, consumed input-event access) were corrected and excluded from the successful-run interval; they are not production errors. The helper was removed and the enabled-mod list restored after testing. Production JAR SHA-256: `3FCACF1086C793F93867D5E3D6F4317F0522588BF7208A3B272AD4B8AA0529BA`. The updated test build remains installed locally. No campaign save was used. Measurements restart on subject changes rather than resuming per-ship cumulative counters.

# Verification

## Maintainability and recording reliability revision

The current local revision compiles for Java 17 and passes 124 assertions using the installed JDK 28+13 and local Starsector/LazyLib dependencies: 19 math/state, 7 segment/circle geometry, 17 obstruction/API fixtures, 24 lifecycle, 19 recording reliability and 38 weapon diagnostics assertions. The two-build packaging regression also passed after the extraction of the new production classes.

Failure injection first reproduced silent archive failure, a live-file failure suppressing the final archive, and listener retention when final snapshot generation threw. The revised recording path reports named failures through the log and combat message area, attempts the archive independently of live-file failure, avoids repeated live-file attempts until a fresh measurement, and guarantees finalization listener cleanup through finally. Panel status distinguishes live failure from archive success/failure; prior archive warnings remain until an archive succeeds. A deliberately broken UI notification no longer interrupts recording cleanup or archiving. The ERROR/WARN entries in these tests are deliberate injected failures, not unexpected test failures.

The per-weapon tests protect firing time, projectile/impact/beam counters, TSV column alignment, HUD/TSV observation consistency, actual FIRING independent of an individual AI request, target provenance, ammunition, range/arc and flux restrictions. Group-mode coverage includes selected, auto, manual and ungrouped weapons; the ungrouped fixture exposed a null lookup into an immutable empty group list, now avoided explicitly. Additional lifecycle checks cover a new projectile before impact, its final impact on removal, and removal without a hit. A failing test also reproduced cooldown being labeled ready. The readout now reports COOLDOWN, FORCED_NO_FIRE and FLUX_HEADROOM_LOW from current observed values. These labels are observations, not a guarantee of AI action or future hits. The overlay focuses on weapon diagnostics; legacy damage file columns remain unchanged and attribution was not expanded.

This revision has not yet been replayed in the game. The gameplay evidence below applies to the earlier three-fix JAR identified by its hash, before this refactor and new recording UI. Failed archives are not persisted in a retry queue. Snapshot-generation exceptions still propagate after listener cleanup; this revision does not claim to catch every failure from an unusual modded weapon.

## Earlier regression checks for shield geometry, rebuilds and combat completion

The patched source was compiled for Java 17 and tested with the installed JDK 28+13 toolchain and local Starsector/LazyLib dependencies. All 64 Java assertions passed: 19 geometry/state, 7 segment/circle geometry, 17 obstruction/API fixture, and 21 lifecycle assertions.

New regressions reproduced the inactive shield-entry/active-exit and post-combat subject-change failures before their fixes. Shield fixtures also cover active entry, starting inside the shield, segment bounds, inactive crossings, zero-length segments, full shields, tangency and disabled shields. Lifecycle fixtures cover a simultaneous combat-end/subject-change, later subject changes, target toggles, off/on input and first enabling after combat completion.

`tests/BuildOutputTest.ps1` reproduced a deleted class surviving a rebuild before the build fix. It now passes with a custom build root: the deleted class and test classes are absent from the production JAR, and an unrelated sentinel file is preserved.

These are local automated checks, not new real-game validation. The earlier gameplay evidence below remains historical.

## Patched-build gameplay checks (2026-10-31)

The patched JAR (SHA-256 `A1557D299B9E26C80FF8168861413067275C7A8E987C07E970881350AAE04482`) was run in Starsector 0.98a-RC8 FR9.6 using the user's Miko_Rouge launcher and JDK 28+13 configuration. The existing Verdant Destroyer Duel exercised the original enabled mod set, including LazyLib, GraphicsLib and Verdant 0.3.5. Off-by-default, enabling, weapon paging, pause/file freeze, paused target/self switching, disabling, fresh-session re-enabling, live FIRING and projectile/impact counters were observed. All 23 Verdant mounts were recorded and only one diagnostic overlay was visible. A natural victory finalized the session and wrote its archive.

A separate temporary mission used public APIs to position actual spawned ships and raise a real Lasher's 210-degree, radius-64 shield. The production diagnostic snapshot identified the Lasher as BLOCKED for a ray above its hull whose shield entry was outside the active arc and exit was inside. The same helper requested engine completion, called the production observer on the actual game thread, changed the actual controlled ship and verified that the final snapshot/session stayed unchanged. The helper intentionally changed test-ship state and directly invoked observer updates; it is not production behavior and is not included in this repository. This was not a full projectile-physics validation or a captured red-row rendering test.

An important pre-existing limitation was confirmed: outgoing damage totals increased, but all per-slot damage stayed zero and all damage was unassigned. A live callback probe reported `com.fs.starfarer.combat.entities.Ship` as the DamageListener source. The observer's projectile/beam source branches therefore did not attribute those events to weapons. Do not treat the per-slot damage columns as validated until attribution is corrected separately.

GraphicsLib's F10 reload binding is a potential overlap, but diagnostic Ctrl+F10 paging worked in this configuration. No new production-diagnostic error was identified in the successful test segments; unrelated RAT/AoTD startup errors also existed in the pre-test log. Temporary test-helper metadata/reflection failures were corrected separately without changing game security settings. No campaign save was opened. Other resolutions, modular ships, fighters, unusual weapon scripts, and other mod/key configurations remain unverified.

## Original 1.0.0 validation

Version 1.0.0 was validated on 2026-10-31 with Starsector 0.98a-RC8 FR9.6 at 2560x1440, LazyLib, and a JDK 28 early-access game runtime. Production targets Java 17. This does not establish compatibility with every game runtime or mod combination.

All 47 automated assertions passed: 19 geometry/state, 7 segment/circle geometry, 6 obstruction transitions, and 15 standalone lifecycle assertions. The arbitrary-ship discovery test failed before the standalone implementation and passed afterward.

Real-game checks: with the Verdant prototype disabled, a vanilla Lasher was detected with seven weapons; weapon paging, live firing/damage, paused target switching to a six-weapon pirate Mule, disable and re-enable were verified. The saved snapshot stopped changing while disabled. With Verdant restored, its 23 mounts were discovered and its embedded panel was suppressed by prototype 0.3.5.

Automatic control transfer and combat-end cleanup were tested with API fixtures, not a real campaign. No campaign save was opened or modified. Modular hulls, unusual weapon scripts, other resolutions and mod key conflicts have not been exhaustively tested. Red obstruction rendering still needs a controlled real-game obstruction test; geometric transition tests passed. No performance improvement is claimed.
