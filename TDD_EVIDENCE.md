# TDD evidence

Actual test runs, newest first. Machine-readable details (every test, its result and the SHA-256 of every test source) are in `docs/tdd/`.

## Gate 3 — green, implementation complete (8 November 2026)

- Evidence: [docs/tdd/green-gate3.json](docs/tdd/green-gate3.json) (results of the runs below, base commit `97d8b1d`, SHA-256 of every test source).
- Environment: as for gate 2 (AVD `ap36`, API 36 `google_apis` x86_64; app compiled against and targeting SDK 37).
- Commands: `./gradlew :core:test :app:verifyFossDependencies`, then `./gradlew :app:connectedFullDebugAndroidTest`, then `./gradlew :app:connectedFossDebugAndroidTest`.
- Added since gate 2: S09 (library export/import round trip). The `full` and `foss` suites each gained this test, so 94 UI tests per flavour.

| Layer | Tests | Passed | Failed | Skipped |
|---|---|---|---|---|
| Core JVM (`core/src/test`) | 160 | 159 | 0 | 1 (opt-in live check) |
| Android UI, `full` | 94 | 93 | 1 | 0 |
| Android UI, `foss` | 94 | 94 | 0 | 0 |
| **Total** | **348** | **346** | **1** | **1** |

Known flaky test: `I14_R33_collectionPageWithFollowAndFilters` (`full`) failed once in this run with a Compose-internal error ("Detected multithreaded access to SnapshotStateObserver" while drawing), not an assertion. It passed in the earlier full runs, in the `foss` run, and in 3 of 3 reruns on its own (`-Pandroid.testInstrumentationRunnerArguments.class=com.jutlandiatech.archiveplayer.ItemUiTest#I14_R33_collectionPageWithFollowAndFilters`).

`verifyFossDependencies` passes, and the `foss` APK's dex files contain no `com.google.android.gms`, `com.google.firebase`, `androidx.mediarouter` or `androidx.media3.cast` references.

Not covered by automated tests (check on real devices, see docs/GETTING_STARTED.md section 8): casting to a physical Chromecast, removable SD cards, the picture-in-picture window buttons, tablet layouts on hardware, performance targets (R77), App Link verification and the GitHub Pages deployment (docs/APP_LINKS.md).

## Gate 2 — red baseline, UI screens added (8 November 2026)

- Evidence: [docs/tdd/red-gate2.json](docs/tdd/red-gate2.json) (replaces the 7 November file; same tool, all test-source hashes re-recorded).
- Adds UI specs and tests for Home (H01–H12), Search (Q01–Q14), Item/collection (I01–I16), Player (P01–P20) and Library (L01–L17), with the `FixtureServer` / `AppEnvironment` / `PlayerProbe` test seams (see TESTING.md).
- Environment: as below, except the emulator is `ap36` = API 36 `google_apis` x86_64 (6 GB data partition). The API 37 image crash-loops in the VM (surfaceflinger assertion `hasReadColorBufferDma`), so installs failed. The app still compiles and targets SDK 37.
- Commands: unchanged (below).

| Layer | Tests | Passed | Failed | Skipped |
|---|---|---|---|---|
| Core JVM (`core/src/test`) | 160 | 0 | 159 | 1 (opt-in live check) |
| Android UI, `full` | 93 | 0 | 93 | 0 |
| Android UI, `foss` | 93 | 0 | 93 | 0 |
| **Total** | **346** | **0** | **345** | **1** |

Why the Android tests fail (checked per test): the screens do not exist yet. 86 per flavour time out waiting for a screen's element ID (`waitForTag`), 7 per flavour (setup and UI Automator tests) fail on a missing ID directly. Core failures are unchanged from 7 November. `verifyFossDependencies` still passes.

## Gate 2 — red baseline (7 November 2026)

- Evidence: [docs/tdd/red-gate2.json](docs/tdd/red-gate2.json), written by `tools/record-tdd-evidence.py`.
- Base: `main` at `10a805d` (approved specification v1.0) plus the test commit on `devin/tests-gate2`.
- Environment: Devin VM (Ubuntu, JDK 17), Gradle 9.9.0, Android SDK 37; emulator `ap37` = API 37 `google_apis` x86_64, phone language en-US.
- Commands:
  - `./gradlew :core:test --continue`
  - `./gradlew --continue :app:connectedFullDebugAndroidTest :app:connectedFossDebugAndroidTest`
  - `./gradlew :app:verifyFossDependencies`

| Layer | Tests | Passed | Failed | Skipped |
|---|---|---|---|---|
| Core JVM (`core/src/test`) | 160 | 0 | 159 | 1 (opt-in live check) |
| Android UI, `full` | 14 | 0 | 14 | 0 |
| Android UI, `foss` | 14 | 0 | 14 | 0 |
| **Total** | **188** | **0** | **187** | **1** |

Why they fail (checked per test, nothing else fails):

- Core: 142 throw `NotImplementedError` from the API skeleton (`TODO()` bodies); 10 `assertThrows` checks receive `NotImplementedError` instead of the expected exception; 5 fail because data files the implementation must add do not exist yet (`app/src/main/assets/texts/TextCatalog_*.json`, `app/src/main/assets/catalog/category-map.json`). Two tests initially passed vacuously on a missing catalog directory; they were tightened before this baseline.
- Android: the screens do not exist yet, so element IDs are missing (`setup_root`, `setup_continue_button`, `setup_language_de` not found).

Structural checks that already pass (not part of the red count): both flavours assemble, and `verifyFossDependencies` confirms the `foss` runtime classpath has no Google Play Services, Cast or MediaRouter (the `full` classpath has `play-services-cast-framework`, `media3-cast` and `mediarouter`).

Gate 3 records `green-gate2.json` with identical test-source hashes once the implementation passes.
