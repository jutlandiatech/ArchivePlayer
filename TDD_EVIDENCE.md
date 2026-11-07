# TDD evidence

Actual test runs, newest first. Machine-readable details (every test, its result and the SHA-256 of every test source) are in `docs/tdd/`.

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
