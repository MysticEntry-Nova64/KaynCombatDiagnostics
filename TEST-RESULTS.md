# Verification

Version 1.0.0 was validated on 2026-10-31 with Starsector 0.98a-RC8 FR9.6 at 2560x1440, LazyLib, and a JDK 28 early-access game runtime. Production targets Java 17. This does not establish compatibility with every game runtime or mod combination.

All 47 automated assertions passed: 19 geometry/state, 7 segment/circle geometry, 6 obstruction transitions, and 15 standalone lifecycle assertions. The arbitrary-ship discovery test failed before the standalone implementation and passed afterward.

Real-game checks: with the Verdant prototype disabled, a vanilla Lasher was detected with seven weapons; weapon paging, live firing/damage, paused target switching to a six-weapon pirate Mule, disable and re-enable were verified. The saved snapshot stopped changing while disabled. With Verdant restored, its 23 mounts were discovered and its embedded panel was suppressed by prototype 0.3.5.

Automatic control transfer and combat-end cleanup were tested with API fixtures, not a real campaign. No campaign save was opened or modified. Modular hulls, unusual weapon scripts, other resolutions and mod key conflicts have not been exhaustively tested. Red obstruction rendering still needs a controlled real-game obstruction test; geometric transition tests passed. No performance improvement is claimed.
