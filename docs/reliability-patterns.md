# Bounded retries and consistent snapshot publication

Java 17 · recording · retries · exception containment · resource lifetime

Recorded 2026-11-01 from the local hardening candidate based on commit
`f1f90ccc1722b4279c346e5cc6fcd1ef57277bd2`. These changes ship in version 1.3.1. Project code is MIT licensed; Starsector and LazyLib are separate
dependencies. This entry describes tested project code, not a general-purpose
durable storage library.

## Problem and assumptions

Previously, a failed archive could be lost when the next ship measurement replaced
the current report. Snapshot construction could also throw through the combat
callback. Cleanup used `finally`, but that alone did not contain the exception or
save the last usable report.

The solution assumes one combat thread owns the recorder and its queue. Reports
are immutable strings. The queue owns no ships, weapons or listeners. Writes are
synchronous and the caller supplies unpaused simulation time. The implementation
is not thread-safe and does not guarantee atomic file replacement or disk durability.

## Preserve the failed operation, not the current mutable state

`DiagnosticRecorder.archive(session, snapshot)` first attempts the write. On
failure, it retains the original filename and text within a bounded queue. A
later measurement must not supply replacement text for that pending session.
Successful retry removes only the saved entry; unrelated failed reports continue
to produce a warning.

The relevant owners are:

- `src/kayn/diagnostics/DiagnosticRecorder.java`: queue, write attempts and status.
- `src/kayn/diagnostics/CombatDiagnostics.java`: scheduling, observation failure
  boundaries, fallback snapshot and listener ownership.
- `tests/ArchiveQueueTest.java`: dependency-free queue regressions.
- `tests/RecordingReliabilityTest.java`: callback, cleanup and real-file recovery.

An example inside the `kayn.diagnostics` package:

```java
Map<String, String> saved = new HashMap<>();
boolean[] diskUnavailable = {true};
DiagnosticRecorder recorder = new DiagnosticRecorder((name, text) -> {
    if (diskUnavailable[0]) throw new IOException("simulated disk failure");
    saved.put(name, text);
}, (operation, name, error) -> System.err.println(operation + ": " + name));

recorder.beginSession();
recorder.archive("example-session", "the original report");
diskUnavailable[0] = false;
recorder.advance(5f);
// saved.get("kayn_combat_diagnostics_example-session.tsv") is the original report.
```

This excerpt needs `java.util.Map`, `java.util.HashMap`, `java.io.IOException` and
the project recorder. Its recovery behavior is exercised by the recorded tests;
it is not a standalone public API.

## Bound memory, work and recovery attempts separately

| Bound | Current policy |
|---|---|
| Entry count | At most eight retained reports |
| Shared text budget | 524,288 UTF-16 characters: up to 1 MiB character payload plus entry overhead |
| Write rate | At most one automatic retry per five unpaused simulation seconds |
| Attempts | Initial write plus four automatic retries |
| Fairness | Failed entry moves behind other eligible entries |
| Exhaustion | Retain the entry and show its exhausted status |
| Explicit recovery | Ctrl+F8 re-enable grants exhausted entries five more attempts |
| Overflow | Reject the new retention request, report loss, preserve older entries |

A large time delta produces one retry, not a catch-up burst. New subjects do not
reset pending reports. Overflow warnings remain for the combat because a later
successful write cannot prove the unqueued report was recovered.

The queue can continue while collection is off and after combat completion only
while the engine still advances the plugin. It does not survive plugin disposal
or game exit. Never reuse this policy for data that requires durable delivery
without adding an appropriate persistent queue and validating its recovery rules.

## Publish a complete snapshot or retain the last complete one

Build the TSV, HUD and per-slot output before publishing them. If construction
fails, stop the measurement and archive the previous complete snapshot with an
explicit `snapshotStale=true` marker. Its original capture time and values remain
historical observations. When no complete snapshot exists for this subject, the
fallback says `noCompleteSnapshot=true` instead of exposing another ship's data.

Catch expected application `RuntimeException` at the observer's input, update and
rendering boundaries. Report context, finish the failed measurement and attempt
all cleanup. Do not catch fatal JVM `Error` values as ordinary diagnostic failures.
Notification/logging failure must not interrupt archive retention or cleanup;
status remains independently represented in the recorder.

Each damage listener also captures its measurement session. An old-session
callback is ignored even if the host's listener-removal API failed. This contains
its effect on later measurements; it does not prove the host released the listener
or its retained objects. Cleanup remains best-effort when the owning API throws.

## Verification and limits

On Windows, using installed JDK 27 with Java 17 compilation target:

- All 222 assertions passed across seven local programs, including 23 queue and
  41 recording-reliability assertions.
- The dependency-free command passed 42 assertions and is configured for CI.
- A real temporary disk file received the exact preserved report after an
  injected first-write failure and automatic retry.
- Failure injection covers startup, refresh, paging, finalization, HUD construction,
  notification, cleanup, pause/off/end scheduling, queue limits and exhaustion.
- Twenty representative weapon scenarios produced identical saved values and
  HUD text before and after the target/readiness refactor.
- The two-build packaging regression and strict documentation build passed.

The escaping snapshot exception was reproduced before containment. Local evidence
is retained in this audit chat's `work/snapshot-red.log`, `work/hardening-tests.log`,
`work/hardening-packaging.log` and `work/characterization/` directory.

Fresh gameplay, hosted CI for this candidate, write latency and performance are
unverified. DeepWiki returned an older implementation and was not used as current
source authority. The retrieved Clean Code Notes exception section informed the
consistent-state approach; these tests establish this implementation's behavior,
not universal correctness of a book rule. Revalidate after changes to session
lifetime, retry timing, file APIs, queue limits or listener ownership.


## Live hardening verification, 2026-11-01

Tested the candidate on Starsector 0.98a-RC8 / Java 28-beta via Miko_Rouge, using a temporary multi-ship mission and test-only snapshot override. Verdant, Lasher and Hammerhead identity, weapon inventory and counter reset checks passed across repeated transfers. An injected snapshot exception stopped observation, detached listeners, preserved the last complete report with a stale marker, and allowed a fresh session.

A locked archive destination exposed that the game can log write/rename failures without throwing. Completed archives now require exact readback before acknowledgement. After unlocking and resuming simulation, the automatic retry recovered the original 4,543-byte report exactly despite a ship change. The final Hammerhead archive also matched its completed live report exactly. The final combat interval contained four expected injected error headers and no unexpected warning/error headers. Earlier setup/launcher errors are outside that interval.

The temporary helper had two premature failure assertions after reading the empty locked-file placeholder at 0.51 simulation seconds. Independent file comparison after further unpaused simulation established successful recovery; those helper assertions are retained as a test-harness limitation, not hidden. This does not establish an exact live five-second timing measurement or broad performance/gameplay compatibility. The local suite passes 222 assertions, including a regression for silent archive write failure. The queue is bounded and in memory; exhausted reports require explicit rearming, and process exit loses pending reports.
