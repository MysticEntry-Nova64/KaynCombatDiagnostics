# Build from source

Requires PowerShell, a JDK 17 or newer, Starsector 0.98a-RC8 and LazyLib installed in the game mods/LazyLib folder. Obtain the game and dependencies separately; their JARs are not redistributed here.

```powershell
./build.ps1 -GameRoot 'D:/Games/Starsector' -JdkRoot 'C:/Program Files/Java/jdk-17'
```

Replace both example paths with your installation paths. The build creates build/ and jars/, compiles for Java 17, runs all four test programs (47 assertions), and produces jars/kayn-combat-diagnostics.jar. Tests and build output are kept outside the production JAR.

To install your build, copy mod_info.json, data/ and jars/ into a single KaynCombatDiagnostics directory under Starsector/mods, then enable Kayn Combat Diagnostics in the launcher.

Tests use local Starsector API fixtures and do not launch the game. See TEST-RESULTS.md for previous real-game validation and its limits.
