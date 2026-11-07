# Specification-first development

Requirements are in SPECIFICATION.md; UI contracts and stable element IDs are in the `UI_*_SPEC.md` files. Status: test gate not started — it begins once the specification is approved.

## Workflow per slice

1. Pick a slice of approved requirements (e.g. language normalization R27/R28, format selection R40–R42, setup U01–U08).
2. Write tests linked to requirement/acceptance IDs, plus the API skeleton they need (unimplemented bodies).
3. Run them and record the failing baseline in TDD_EVIDENCE.md and `docs/tdd/red-<slice>.json`.
4. Implement the minimum to make them pass. Never weaken a test to get green.
5. Re-run, record `green-<slice>.json` with identical test-source hashes, then build and lint both flavours.
6. Defects found later get a failing regression test before the fix.

## Test layers

| Layer | Where | What it proves |
|---|---|---|
| Core unit tests | `core/src/test` (JUnit, JVM) | Domain rules with fake inputs and injected time: catalog/query building, language normalization, adult filter, format/quality choice, resume rules, sleep timer, playlist codec and share-link limits, recommendations, settings independence (A42), download-location policy (R85). Fast, no emulator. |
| Contract tests | `core/src/test` with recorded archive.org JSON fixtures | Parsing of real advancedsearch/metadata responses, including missing/odd fields. |
| Live catalog checks | separate, opt-in task | Every category-map query returns enough playable items. Not part of the default build (network-dependent). |
| Android UI tests | `app/src/androidTest` (Compose test + UI Automator) | Screens, navigation and Settings by element ID (`testTag`, exposed as resource IDs), in all three languages, phone and tablet sizes, both flavours. |
| Flavour checks | Gradle tasks | `foss` contains no Google Play Services / Cast classes; `full` shows Cast. |
| Device checks | manual / scripted on a phone | Playback, Cast to a real Chromecast, downloads to SD card, PiP, backup/restore. |

## Naming and traceability

Test names start with the ID they cover, e.g. `R27_appLanguageChangeKeepsContentLanguages`, `A44_switchLocationOffersMove`, `U05_contentLanguagePreselectionFromPhone`, `S03_...`. A traceability table (ID → tests → latest result) is added here as slices land. A requirement is only marked covered when its acceptance scenario passes at the right layer; a passing core test does not prove Android behaviour.

## Running

```powershell
.\tools\run-gradle.ps1 :core:test
.\tools\run-gradle.ps1 :core:build :app:assembleDebug :app:lintDebug
```

Instrumentation and flavour tasks are added with the first UI slice.

## Evidence

Actual commands, counts and results go in TDD_EVIDENCE.md, never here.
