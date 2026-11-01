# Bounded shot history and independent HUD restrictions

Java 17 / Starsector 0.98a-RC8 / LazyLib. Recorded 1 November 2026 from the
local 1.4.0 implementation on `feature/weapon-evidence-vectors`, based on
v1.3.1 commit `29bd38e`. Project code is MIT licensed; game APIs and LazyLib
are separately licensed and are not copied into this entry.

## Ownership and use

`WeaponDiagnostics` owns one `ShotHistory` per weapon. Combat observation
deduplicates projectile identities before recording them. A session/subject
change creates fresh weapon rows, excluding projectiles already airborne.
The diagnostic reads combat evidence; it does not issue targeting, firing,
steering, weapon-state or AI commands.

`ShotHistory.add(origin, facing)` validates finite inputs, copies scalar world
coordinates into an immutable `Line`, inserts newest first, and removes the
oldest beyond capacity five. `lines()` returns an immutable list snapshot.
Each line is 90 world units long. Age colours are RGB `(0,255,0)`, `(0,180,0)`,
`(0,100,0)`, `(0,40,0)`, `(0,0,0)`. Age advances with a new observed projectile,
not elapsed time. Rejected input does not age or evict valid entries.

Example inside the diagnostics package:

```java
ShotHistory history = new ShotHistory();
history.add(new Vector2f(12, 34), 0);
// Newest line starts at (12,34), ends at (102,34), and is bright green.
List<ShotHistory.Line> frozen = history.lines();
// Later additions do not change frozen or any stored coordinates.
```

This example uses the project's existing class and LWJGL Vector2f dependency;
it is not a standalone dependency-free library. Canonical implementation:
`src/kayn/diagnostics/ShotHistory.java`, `WeaponDiagnostics.java`,
`CombatDiagnostics.java`, and `ShotVectorRenderer.java` in the source checkout.
Draw oldest first so coincident newest lines remain bright. Rendering stores
no game entities in history and preserves GL state.

## Restrictions and rendering

`WeaponRestriction.observe(...)` evaluates independent facts into an EnumSet
and publishes an immutable list. Avoid an else-if chain that hides simultaneous
restrictions. Capture the list once per complete refresh; both TSV and HUD use it.
For example, Blocked and Cooldown may appear together even when the independent
weapon API reports FIRING. Unknown evidence must not become an invented reason.

Labels and colours belong to the restriction model. `DiagnosticPanel.spans(...)`
turns internal colour markers into text/colour spans, for example orange Blocked,
a white separator and blue Cooldown. It does not infer new restrictions.

The first live candidate's mixed-colour LazyFont append path rendered white and
showed a font artifact despite passing span tests. Individual plain-text draws
with a base colour per span, client-state preservation and an isolated
GL_COLOR_ARRAY state corrected the observed rendering in a fresh process.
The responsible component within the installed graphics stack was not isolated;
do not treat this as proof of a general LazyLib or Fast Rendering defect.

## Verification and limits

Existing evidence, not rerun during this documentation task:

- Eight local programs reported 291 passing assertions on JDK27.0.1 targeting
  Java17, including bounded eviction, colour order, detached coordinates,
  immutable publication, independent weapons, duplicate events, pause/reset,
  restriction combinations and invalid geometry. Portable checks reported42.
- Live Miko Java28 test in A Fistful of Credits, with the user's enabled mods:
  Lasher firing/counters, selected Mule and return, green line rendering,
  grey NoTarget, orange Blocked with blue Cooldown, and paused cached evidence.
- Final fixed-footer, missing/nonfinite-muzzle and new-session-HUD follow-ups
  were fixture/build checked after the live run, not rerun in-game.

Each projectile/pellet is an entry, not a proven trigger pull. Missing spawn or
facing data produces no line. Continuous beams retain duration/contact evidence;
missiles show first-observed facing, not a predicted trajectory. Black,
overlapping, offscreen or panel-covered lines may be difficult to see. Exact
five-shade/eviction rules were deterministically checked, not separately
instrumented live. No performance gain, exhaustive mod compatibility or full
window/scaling coverage is established.

Canonical evidence and runnable source snapshot:
`C:/Users/Minsu/Documents/Codex/2026-11-01/kayn-diagnostics-planning/outputs/IMPLEMENTATION-AND-VERIFICATION.txt`,
`build-and-package.log`, `live-check/`, and
`KaynCombatDiagnostics-1.4.0-source.zip`.
Focused checks: `tests/WeaponEvidenceTest.java`, `WeaponDiagnosticsTest.java`,
and `DiagnosticsIntegrationTest.java`. Revalidate in the consuming runtime
after game, graphics-stack, dependency or event-semantics changes.

## Display cleanup — implemented in 1.4.1

The user identified duplicate white NO_TARGET and grey NoTarget labels in an
annotated screenshot. Keep the grey restriction; remove the redundant white
absence label. Show known target type/source once, keeping control mode separate.

Agreed compact header:

```text
COMBAT DIAGNOSTICS | 49s | Recording
Mule (P) · Selected ship
Hull 5498 · Flux 0 / 6400 · Weapons 6
PAUSED · Evidence age 0.24s
```

Remove the descriptive purpose line. Hide page numbering for a single page;
show it when multiple pages exist. Move shortcuts into compact footer/help.
The timer remains observed simulation time, frozen during pause; recording
failures replace the normal recording status with an explicit warning.
These changes are implemented in 1.4.1. The earlier 1.4.0 evidence above retains
its original scope. Header formatting belongs to DiagnosticHud; recording summary
is read live so file failures and retry recovery cannot leave a healthy cached
Recording label. Current verification is recorded in TEST-RESULTS.md and the
compact-hud report under the task outputs.

## 1.5.0 standalone board boundary

Current code removes LazyLib; the historical workaround above retains its dated
scope. `BitmapFontMetrics` parses immutable installed font metadata;
`PanelLayout` computes content-sized bounds without GL; `HudFont` draws only;
`RenderState` owns GL preservation. No shared SpriteAPI properties are changed,
no game texture is deleted and no proprietary font is bundled. The resource is
explicitly loaded via SettingsAPI; BMFont Y coordinates are converted at the
render adapter. `ObservationFreshness.State` drives the footer directly instead
of searching arbitrary text. `DiagnosticKeys` validates unique configured keys.

The boundaries principle from the user's Clean Code Notes informed this split:
keep external API/resource details behind a narrow adapter and test pure logic
separately. That is design guidance, not evidence of a measured performance gain.
Live rendering is a separate acceptance check: fixtures cannot detect unsupported
Fast Rendering bridge calls, unloaded textures or wrong texture orientation.
Current verification and known limits are in TEST-RESULTS.md.
