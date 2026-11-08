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
- [UI_HOME_SPEC.md](UI_HOME_SPEC.md) — Home rows, cards and refresh (H01–H12). Draft, awaiting approval.
- [UI_SEARCH_SPEC.md](UI_SEARCH_SPEC.md) — search, suggestions, filters, sorts, browse and category pages (Q01–Q14). Draft, awaiting approval.
- [UI_ITEM_SPEC.md](UI_ITEM_SPEC.md) — item and collection pages, entry rows and menus (I01–I16). Draft, awaiting approval.
- [UI_PLAYER_SPEC.md](UI_PLAYER_SPEC.md) — mini and full player, sheets, queue, PiP, errors (P01–P20). Draft, awaiting approval.
- [UI_LIBRARY_SPEC.md](UI_LIBRARY_SPEC.md) — Library, playlists, likes, follows, downloads, history, share/import (L01–L17). Draft, awaiting approval.
- [ANDROID_FEASIBILITY.md](ANDROID_FEASIBILITY.md) — archive.org, Cast and Android findings and validation gates.
- [docs/ARCHITECTURE_UML.md](docs/ARCHITECTURE_UML.md) — proposed module/component architecture.
- [TESTING.md](TESTING.md) — test strategy, traceability and how to run checks.
- [TDD_EVIDENCE.md](TDD_EVIDENCE.md) — recorded red→green runs (evidence files in `docs/tdd/`).
- [DEVELOPMENT_ENVIRONMENT.md](DEVELOPMENT_ENVIRONMENT.md) — observed build environment and toolchain.

Current status: specification v1.0 approved (7 November 2026). Gate 2 red baseline recorded: 346 tests written before the implementation (160 core, 93 Android UI per flavour), all failing or skipped against API skeletons (see TDD_EVIDENCE.md). The app module is still a scaffold (single sample stream; Cast only in the `full` flavour). Next: gate 3, implementation.

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
