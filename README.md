# ArchivePlayer

An open-source Android app for discovering, streaming and casting the audio and video hosted on the [Internet Archive](https://archive.org).

## License

Licensed under the [Apache License 2.0](LICENSE). ArchivePlayer is independent and not affiliated with the Internet Archive.

## Specification

ArchivePlayer is developed specification-first: specifications are agreed before tests, and tests are written before the implementation they cover.

- [SPECIFICATION.md](SPECIFICATION.md) — authoritative requirements (R…), acceptance scenarios (A…), resolved points and decision log (D…).
- [UI_SHELL_SPEC.md](UI_SHELL_SPEC.md) — navigation shell, mini-player, theme toggle, flavour differences, element-ID contract.
- [UI_SETUP_SPEC.md](UI_SETUP_SPEC.md) — first-launch setup.
- [UI_SETTINGS_SPEC.md](UI_SETTINGS_SPEC.md) — Settings menu and detail screens.
- [ANDROID_FEASIBILITY.md](ANDROID_FEASIBILITY.md) — archive.org, Cast and Android findings and validation gates.
- [docs/ARCHITECTURE_UML.md](docs/ARCHITECTURE_UML.md) — proposed module/component architecture.
- [TESTING.md](TESTING.md) — test strategy, traceability and how to run checks.
- [TDD_EVIDENCE.md](TDD_EVIDENCE.md) — recorded red→green runs (evidence files in `docs/tdd/`).
- [DEVELOPMENT_ENVIRONMENT.md](DEVELOPMENT_ENVIRONMENT.md) — observed build environment and toolchain.

Current status: specification v0.2 in final review. No feature tests or production features yet; the app module is a scaffold (single sample stream with Cast).

## Design

[design/](design/README.md) holds the interactive HTML mockup (open `design/archiveplayer-mockup.html` in any browser) and the "Signal A" logo masters.

## Build

Open the project in Android Studio, or use the helper from a PowerShell terminal:

```powershell
.\tools\run-gradle.ps1 :core:test
.\tools\run-gradle.ps1 :core:build :app:assembleDebug :app:lintDebug
```

VS Code users can open [ArchivePlayer.code-workspace](ArchivePlayer.code-workspace) and use **Terminal → Run Task**.

## Code map

- `core/src/main/kotlin/com/thomasnorlund/archiveplayer/core` — pure Kotlin/JVM domain logic (no Android dependency): catalog model, query building, format selection, language normalization, filtering, playlist codec, recommendations, settings and download policies.
- `core/src/test/kotlin/...` — specification-linked JUnit tests, named after requirement/acceptance IDs.
- `app/src/main/java/com/thomasnorlund/archiveplayer/ui` — Compose screens, theme and stable element IDs.
- `app/src/main/java/com/thomasnorlund/archiveplayer/playback` — Media3 player, session and (in `full`) Cast integration.
- Planned in the implementation gate: `setup/`, `data/` (archive.org client, Room, DataStore), `platform/` (downloads, storage volumes, backup), `app/src/main/assets/texts` (TextCatalog JSON), `app/src/full` / `app/src/foss` flavour source sets, `app/src/androidTest` (Compose/UI Automator checks by element ID).
- `design` — approved design references and logo masters.
- `docs` — architecture and TDD evidence.
- `tools` — terminal and build helpers.
- `dist` — tested APKs with build/signature evidence (created at the first release).
