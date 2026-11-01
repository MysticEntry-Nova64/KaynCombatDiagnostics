## 1.5.0 audit and verification — 1 November 2026

Ten local programs report **333 passing assertions**, zero failed assertions:
19 math/state, 23 archive queue, 8 geometry, 17 geometry/API, 32 lifecycle,
44 recording reliability, 109 weapon diagnostics, 51 weapon evidence,
22 font/layout/key checks and 8 portable font checks. JDK27.0.1 targets Java17.
Portable checks pass 50; expected injected failures in recording tests are
distinct from live game errors. Build-output regression checks deleted/test
classes cannot leak into runtime packaging and unrelated build files survive.

### Code and dependency boundaries

- Production JAR contains only `kayn.diagnostics` classes. No external mod/library
  dependency in metadata or build classpath. Jdeps resolves Java, Starsector API,
  and the game's bundled Log4j/LWJGL only; LazyLib is no longer referenced.
- Font parser, board layout, shortcuts, freshness, restrictions, geometry,
  shot history and recording have separate modules. Game integration remains an
  opt-in observer. Tests reject combat API setter/force/spawn/command calls.
- Source review found no steering, targeting, damage, weapon/stat mutators,
  game-class overrides or original data/model changes. The observer registers
  its own listeners/plugin/custom-data key, writes its own files, consumes only
  configured Ctrl shortcuts, and saves/restores drawing state.
- SHA256 comparison found all 21 original core JARs unchanged. Other mods were
  not edited. Original enabled_mods.json was restored byte for byte; the temporary
  verification mod was moved outside the game after testing.

### Real game checks

Launched through Miko_Rouge.bat and verified the Java28 executable. Starsector
0.98a-RC8 with the user's existing enabled mod stack, including Fast Rendering.
Normal tactical UI transferred Hammerhead → Sunder → Wolf, with command shuttle
sessions in between. The observer followed automatically and reset counters;
separate subject archives were read back. Target mode pinned a selected
Dominator and returned to the controlled Hammerhead. No-target toggle remained
on the controlled ship. Tested pages, help open/close, scrolling shortcuts,
paused evidence, disable/re-enable, ship loss and combat completion. Explicit
stop produced an ended=true/reason=disabled archive; combat-end Wolf archive
contains reason=combat_ended. Snapshot readback verification is enabled in code.

Fresh native screenshots show the rounded teal board, readable independent
colours, conditional pages, firing activity, projectile/impact counters, initial
green-to-dark shot vectors, beam time/contact counters and freshness. Live
restrictions observed include Distance, Arc, NoTarget, Cooldown, NoAmmo, Damaged
and Fluxing. Deterministic fixture tests cover every restriction and combinations,
geometry guards, five-entry eviction/shades, target provenance and failure paths.

Final fresh-process combat interval: **0 ERROR / 0 WARN**. Entire final startup:
32 ERROR / 8 WARN from the existing mod stack; no diagnostic errors. This does
not certify the whole installation as error-free or those other errors as harmless.

### Failures found and corrected before release

Large finite geometry could overflow float intermediates and falsely miss a
circle intersection; a failing regression passed after double intermediates.
Fast Rendering rejected glClientActiveTexture; removed that unnecessary call
for immediate-mode drawing. Atlas loading required loadTexture before getSprite,
and BMFont top-origin coordinates required conversion to the game's bottom-origin
texture coordinates. Fresh-process visual reruns verified the final rendering.
The first media helper used reflection proxies rejected by the game's script
security boundary. Replaced it with plain labelled display examples; no security
settings were bypassed. It is test-only and excluded from the runtime package.

### Media and limits

24 animated GIFs were encoded from fresh complete native screenshots, reopened
to verify frame counts, and visually checked through contact sheets/keyframes.
Six live clips cover metrics, firing/impacts, vectors, subjects, page/help/pause,
and target pinning. Eighteen display illustrations cover all 12 restriction
labels, combinations, beam/projectile counters, freshness, recording warning
and help. They are explicitly labelled illustrations, not live measurements;
their example counters are deliberately artificial. See the media gallery.

No exhaustive compatibility guarantee, performance improvement, high-DPI/window
matrix coverage, trigger-pull counting or validated per-weapon damage/accuracy
metric is claimed. Black/overlapping/offscreen lines may be hard to see. Game
target labels drawn later can overlap the board. Rare disk failures and FAILED/
STALE states are deterministic fixture checks, not forced failures in a campaign.
Pending archive retries remain combat-local memory, not a durable spool.

Canonical local evidence: `outputs/audit-release/` in the planning workspace:
build/portable/package logs, dependency reports, native screenshots and freshness
sidecars, copied archives, log-summary, media source manifests and GIFs.

## 1.4.1 compact HUD — 1 November 2026

Eight local programs passed 302 reported assertions with zero failed assertions:
19 math/state,23 archive queue,7 geometry,17 geometry/API,32 lifecycle,
44 recording reliability,109 weapon diagnostics,51 evidence/HUD checks.
Build uses JDK27.0.1 targeting Java17 and installed game/LazyLib APIs.
Expected injected ERROR logs remain in the recording failure-path tests.
Portable checks passed42. The HUD-specific checks exercise conditional page
display, grouped metrics, concise recording/failure state, one NoTarget label,
help toggle and independent pause status in the actual HUD composition.

Live candidate: Miko_Rouge.bat, verified Java28 process18188, standalone
A Fistful of Credits with the user's enabled mods. Opened fresh native captures
show compact title/timer/Recording, Lasher Page1/2 then Page2/2, help open/closed,
PAUSED status, selected six-weapon Mule with no Page1/1, one grey NoTarget,
and independent red NoAmmo/Damaged, amber Arc and purple AIHold observations.
Normal game autopilot produced projectile activity; diagnostics remained read-only.

Log scope: the latest launcher banner starts at original line228020. This run
has32 startup ERROR and8 WARN headers from other mods; all32 error messages also
occur in the previous run. No diagnostics errors. From combat start182923ms
through the recorded test, there are0 ERROR,0 WARN and0 FATAL headers. Old appended
runs contain historical injected errors and were excluded from current-run counts.

Verification limit: the final Ctrl+F8 archive input was blocked after user input
was detected. The test process was then stopped before archive confirmation;
that automation step did not pass and the current exit archive is unverified.
Live reports and screenshots remain available. This is not a passing exit-archive
test; existing automated archive tests did pass. Broad mod/window/scaling and
performance compatibility remains unverified.

Evidence: C:/Users/Minsu/Documents/Codex/2026-11-01/kayn-diagnostics-planning/outputs/compact-hud/.
Fresh selected-ship capture:live-check/snapshot-20261101-111851-858-7de18f7e.png.
Help/page2 capture:live-check/snapshot-20261101-111459-158-86ddef9d.png.
Prior verification below retains its original version/date and limits.

## 1.4.0 weapon evidence — 1 November 2026

Implemented from clean v1.3.1 commit 29bd38e on local branch
feature/weapon-evidence-vectors. No remote publication.

Eight local test programs pass 291 reported assertions on JDK 27.0.1, compiling
with --release 17 against installed Starsector 0.98a-RC8 and LazyLib APIs:
19 math/state, 23 archive queue, 7 geometry, 17 geometry/API transitions,
28 lifecycle, 43 recording reliability, 110 weapon diagnostics, 44 new evidence.
Injected ERROR stack traces in recording tests are expected failure-path checks.
Portable checks pass 42 assertions. Two-build packaging regression passes.

New checks exercise simultaneous independent restrictions/colour spans, five-line
eviction and monotonic colour darkening, detached coordinates and immutable
history, independent weapons, projectile spawn-versus-current location, duplicate
observation, pause/session reset, invalid muzzle/aim/range, collinear overlap,
endpoint contact, and freshness/fixed-footer status. Existing provenance,
snapshot publication, cleanup and archive retry checks remain in the full suite.

Live smoke test used Miko_Rouge.bat and verified Java executable
Starsector/jdk-28+13/bin/java.exe, PID 15688 for the corrected renderer run.
The vanilla A Fistful of Credits mission ran with the user's enabled mod set;
this was not a vanilla-only installation. Observed Lasher collection, normal
game autopilot firing, projectile counters, initial-direction history rendering,
PAUSED / STALE with cached FIRING, selected Mule observation and return to self.
Mule HUD showed orange Blocked and blue Cooldown together; NoTarget was grey.
Fresh native game screenshots were opened, not inferred from timestamps.

First live candidate rendered coloured labels white and showed a font artifact.
Using individual single-colour text draws and preserving client GL state corrected
the observed display in a fresh process. Mixed-colour font buffers were the
affected path; the underlying graphics-stack cause was not established.

The final fixed freshness footer, missing/nonfinite muzzle follow-up and new-session
HUD reset were made after that live run; fixture/build checks cover the final
candidate, but those small follow-ups were not rerun in-game. Full small-window/
scaling combinations, every label in live combat, all modded weapons, all five
line shades/eviction via controlled live instrumentation, and performance remain
unverified. Exact history/colour/eviction rules are verified in deterministic tests.
Black lines and overlapping histories can be difficult to distinguish visually.
Each observed projectile (including pellets) counts separately; beams are not
invented shots; missile lines are initial observed directions, not trajectories.

Evidence and distributable artifacts:
C:/Users/Minsu/Documents/Codex/2026-11-01/kayn-diagnostics-planning/outputs/
See IMPLEMENTATION-AND-VERIFICATION.txt, test logs and live-check/ captures/reports.
Historical results below retain their original versions and limits.

## Unreleased hardening verification (2026-11-01, local candidate)

This working revision is newer than published 1.3.0. Production Java was changed;
the game installation and GitHub release assets were not changed. No new game
session or hosted GitHub check was run for this candidate.

All seven local test programs passed 222 assertions with JDK 27 targeting Java 17:
19 math/state, 23 archive queue, 7 line geometry, 17 obstruction/API,
24 lifecycle, 41 recording reliability and 89 weapon diagnostics. Failure logs
are deliberate injected exceptions. The original escaping snapshot exception
was reproduced before containment and is preserved in the local audit evidence.

Target provenance and readiness priority now have separate owners. A differential
harness compared this implementation with committed 1.3.0 production code across
20 scenarios: TSV values and HUD text matched byte for byte. This is behavioral
characterization, not exhaustive compatibility or a performance measurement.

Snapshot failures at initial collection, refresh, pagination, finalization and
HUD construction are contained. Fallback reports identify stale/incomplete data;
publication waits for complete TSV and HUD construction. A real temporary disk
file confirmed automatic recovery of the exact queued report after an injected
write failure. Fixtures also verify paused/off/post-combat retry behavior,
re-enable recovery, bounded size/attempts, fairness, notification failure,
persistent overflow warnings and exhausted entries. A failed host listener
removal is contained; old-session callbacks cannot affect a new measurement.

The two-build packaging regression passed after the refactor. The dependency-free
command passed 42 assertions locally. Queue memory is bounded to eight reports
and 524,288 UTF-16 characters of text; retries are one write per five unpaused
simulation seconds. The queue is combat-local and not durable across plugin
disposal or game exit. Five attempts are allowed initially; explicit re-enable
grants exhausted entries another five attempts. Fatal JVM Errors are not caught.
Synchronous file-write latency and live gameplay remain unmeasured/unverified.

The following sections describe earlier revisions and their original limits.

## v1.3.0 release-package preparation (2026-11-01, local review)

README now states that the individual AI firing request is in TSV `aiShouldFire`,
not a separate HUD field. Production Java is unchanged from tagged v1.3.0.
The full local build passed all 178 assertions with JDK 27, targeting Java 17.
The new dependency-free CI command passed its 19 math/state assertions locally.
Strict documentation generation and checks of four pages/search content passed;
the documentation environment's dependency check passed.

The binary ZIP validator passed for the 1.3.0 package, including its manifest,
plugin registration, allowlisted documentation/runtime files, production entry
class and Java 17 bytecode. Deliberately missing-JAR, unexpected-file and wrong-version
archives were rejected. The packaging command refused to overwrite an existing
release and preserved its checksum. The package contains no proprietary dependencies.

GitHub Actions configuration is prepared for documentation and dependency-free
Java checks on pushes and pull requests, using pinned action revisions and read-only
repository permissions. Its YAML was parsed locally; no hosted Actions execution
has occurred before push. Full game-dependent tests remain local. No new game launch,
game installation change, tag movement, push or release publication was performed
for this preparation. Earlier gameplay evidence below retains its original limits.

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


## Live hardening verification, 2026-11-01

Tested the candidate on Starsector 0.98a-RC8 / Java 28-beta via Miko_Rouge, using a temporary multi-ship mission and test-only snapshot override. Verdant, Lasher and Hammerhead identity, weapon inventory and counter reset checks passed across repeated transfers. An injected snapshot exception stopped observation, detached listeners, preserved the last complete report with a stale marker, and allowed a fresh session.

A locked archive destination exposed that the game can log write/rename failures without throwing. Completed archives now require exact readback before acknowledgement. After unlocking and resuming simulation, the automatic retry recovered the original 4,543-byte report exactly despite a ship change. The final Hammerhead archive also matched its completed live report exactly. The final combat interval contained four expected injected error headers and no unexpected warning/error headers. Earlier setup/launcher errors are outside that interval.

The temporary helper had two premature failure assertions after reading the empty locked-file placeholder at 0.51 simulation seconds. Independent file comparison after further unpaused simulation established successful recovery; those helper assertions are retained as a test-harness limitation, not hidden. This does not establish an exact live five-second timing measurement or broad performance/gameplay compatibility. The local suite passes 222 assertions, including a regression for silent archive write failure. The queue is bounded and in memory; exhausted reports require explicit rearming, and process exit loses pending reports.
