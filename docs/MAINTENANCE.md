# Maintenance guide

## Find the code

| Concern | Primary source |
|---|---|
| Setup, navigation, library and playback coordination | `app/src/main/java/com/jutlandiatech/archiveplayer/AppViewModel.kt` |
| Screens and stable element IDs | `app/src/main/java/com/jutlandiatech/archiveplayer/ui/` |
| Parsers, queries, format selection, codecs and policies | `core/src/main/kotlin/com/jutlandiatech/archiveplayer/core/` |
| Networking and persistence | `app/src/main/java/com/jutlandiatech/archiveplayer/data/` |
| Session/service and controller | `app/src/main/java/com/jutlandiatech/archiveplayer/playback/` |
| Cast variants | `app/src/full/` and `app/src/foss/` |
| Catalog/text/fixture generation | `tools/gen_category_map.py`, `tools/gen_text_catalogs.py`, `tools/gen_ui_fixtures.py` |
| Website deployment | `.github/workflows/pages.yml` and `site/` |

## Change and verify

Follow the [specification-first workflow](../TESTING.md): identify requirement/acceptance IDs, record failing tests for behavioral changes, implement, rerun and record evidence. Documentation corrections alone do not need invented application red/green tests. Preserve historical evidence and append actual new measurements with dates and environment.

Use [getting started](GETTING_STARTED.md) for SDK/JDK setup. Run from the repository root in PowerShell:

```powershell
.\tools\run-gradle.ps1 :core:test
.\tools\run-gradle.ps1 :app:assembleFullDebug :app:assembleFossDebug :app:lintDebug
.\tools\run-gradle.ps1 :app:verifyFossDependencies
```

With an emulator or connected test device:

```powershell
.\tools\run-gradle.ps1 --continue :app:connectedFullDebugAndroidTest :app:connectedFossDebugAndroidTest
```

Core reports are under `core/build/reports/tests/test/`; Android reports under `app/build/reports/`. Instrumentation uses a local fixture server and an orchestrator that clears application data. Use a disposable test installation.

For policy/codec changes, run the relevant core tests. For Android/UI changes, run matching instrumentation and affected flavour builds. Cast changes need dependency separation and receiver conversion checks, plus physical handover validation. Storage changes need missing/remounted media and interrupted transfer/move checks. Emulator evidence does not establish physical receiver or SD-card behavior.

If Gradle fails before tests execute, record the toolchain/dependency failure rather than a test result. Current settings can differ from historical environment notes. Source version comes from `app/build.gradle.kts`; verify packaged APK versions and checksums independently before distribution.

## Maintain the documentation

Keep the [index](README.md) and root [README](../README.md) navigable. Update [UML](ARCHITECTURE_UML.md) when class ownership, persistence, service boundaries or flows change. Mermaid diagrams are editable text rendered by GitHub; preview them after changes, since source checks alone do not prove layout.

Requirements describe agreement, architecture describes implementation, and evidence records measurements. Explain discrepancies rather than silently treating a plan or old result as current behavior.
