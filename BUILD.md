# Build from source

Requires PowerShell, a JDK 17 or newer, Starsector 0.98a-RC8 with no external mod libraries. Obtain the game separately; its JARs are not redistributed here.

```powershell
./build.ps1 -GameRoot 'D:/Games/Starsector' -JdkRoot 'C:/Program Files/Java/jdk-17'
```

Replace both example paths with your installation paths. The build creates build/ and jars/, compiles for Java 17, runs all ten test programs, and produces jars/kayn-combat-diagnostics.jar. Tests and build output are kept outside the production JAR.

Each invocation compiles into a new `run-<UUID>` directory under `build/` (or your custom `-BuildRoot`). Only that invocation's production classes are packaged, so removed or renamed classes cannot leak into a rebuilt JAR. Earlier workspaces are retained for inspection; the build does not delete unrelated files in your build directory.

To run the packaging regression, which builds an isolated fixture twice and checks that a deleted class and test classes are absent from the rebuilt JAR:

```powershell
./tests/BuildOutputTest.ps1 -GameRoot 'D:/Games/Starsector' -JdkRoot 'C:/Program Files/Java/jdk-17'
```

The fixture is retained under `build/` and does not modify the installed mod or game.

To install your build, copy mod_info.json, data/ and jars/ into a single KaynCombatDiagnostics directory under Starsector/mods, then enable Kayn Combat Diagnostics in the launcher.

Tests use local Starsector API fixtures and do not launch the game. See TEST-RESULTS.md for previous real-game validation and its limits.

## Automated checks

`.github/workflows/checks.yml` runs on pushes, pull requests and manual dispatch.
It builds documentation strictly, checks generated/searchable content, and compiles
and runs 50 dependency-free math/state, geometry, font and archive-queue assertions on Java 17.
Run the Java check locally with `./tests/PortableChecks.ps1`; run
`python tests/check_docs.py site` after the documentation build.

The complete local fixture suite and two-build regression still require the local
game API/runtime. Public CI does not download or redistribute them.
The workflow has read-only repository permissions and does not publish releases.
Workflow configuration follows the [GitHub Actions documentation](https://docs.github.com/en/actions/get-started/quickstart).

## Prepare a binary release

From a full source checkout:

```powershell
./package-release.ps1 -GameRoot 'D:/Games/Starsector' -JdkRoot 'C:/Program Files/Java/jdk-17'
```

This builds and runs the complete local fixture suite, stages only the runtime files and user
documentation, validates the ZIP, and writes `dist/KaynCombatDiagnostics-1.5.0.zip`
and its `.sha256` file. Existing release outputs are preserved; select a fresh
`-OutputRoot` when rerunning. Staging directories remain under ignored `build/`.
The package excludes proprietary dependencies, tests, captures and local data.

Validate an existing ZIP with
`./tests/ReleasePackageTest.ps1 -ZipPath './dist/KaynCombatDiagnostics-1.5.0.zip' -ExpectedVersion '1.5.0'`. If omitted, `-ExpectedVersion` defaults to the version
in the checkout's `mod_info.json`; supply it explicitly when checking an older release.
RELEASE-NOTES.md contains installation and release details. The binary ZIP omits
source and build scripts; obtain the full checkout from GitHub to rebuild it.

Preparing files does not upload them. Review changes and test results before
pushing. The existing v1.3.0 tag identifies the original source publication;
do not move it silently. A later release upload should disclose any documentation
or packaging changes made after that tag while keeping its production Java provenance clear.
