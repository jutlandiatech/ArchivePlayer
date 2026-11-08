# Specification-first development

Requirements are in SPECIFICATION.md; UI contracts and stable element IDs are in the `UI_*_SPEC.md` files. Status: **gate 2 red baseline recorded** (8 November 2026): 346 tests written before any feature code, all failing or skipped against unimplemented skeletons. See TDD_EVIDENCE.md.

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

Android UI tests (both flavours, needs a running emulator or phone) and the flavour check:

```powershell
.\tools\run-gradle.ps1 --continue :app:connectedFullDebugAndroidTest :app:connectedFossDebugAndroidTest
.\tools\run-gradle.ps1 :app:verifyFossDependencies   # also part of :app:check
```

Opt-in live catalog check (network): `.\tools\run-gradle.ps1 :core:test --tests "*LiveCategoryMapCheck*" -Darchiveplayer.live=true` (system property must reach the test JVM; wired up in the implementation gate).

UI tests run with the test orchestrator and `clearPackageData`, so every test starts from a fresh install. The emulator's phone language is en-US; other phone languages are covered by core tests (A01, A02, U01).

## Traceability (red baseline)

| Area | Test class (layer) | IDs |
|---|---|---|
| Text catalogs | `TextCatalogTest`, `ShippedCatalogsTest` (core) | R65, R66, A03, A04, R72, R27, UI_SETUP text contract |
| Languages | `LanguagesTest` (core) | R13, R28, R67, R83, A01, A02, A05–A07, U01, SO1, SO2 |
| Settings and theme | `AppSettingsTest` (core) | R27, R72, A38, A42, R82, D30 |
| Setup flow | `SetupFlowTest` (core), `SetupUiTest` (Android) | U01–U07, A42 |
| archive.org parsing | `MetadataParserTest`, `CatalogUtilitiesTest` (core, recorded fixtures) | R07–R12, R23, R45, R78, A10–A12, A33, A40 |
| Format selection | `FormatSelectorTest` (core) | R09, R40, R41, R43, R49, A13–A15 (A13 includes the R42 quality menu) |
| Category map | `CategoryMapTest`, `BundledCategoryMapTest`, `LiveCategoryMapCheck` (opt-in) (core) | R14, R15, R17, R68, A08, A09 |
| Adult filter | `AdultFilterTest` (core) | R71, A27 (R70 default), A41 |
| Search | `SearchTest` (core) | R07, R22–R26, R28, R29, R71, A06, A07, A10 |
| Network politeness | `ArchiveClientTest` (core) | R75, R76, A35 |
| Playback rules | `PlaybackTest` (core) | R35–R37, A16, A17, A34 |
| Playlists, sharing, export | `PlaylistsTest` (core) | R51, R52, R54, R57, R58, R74, A21, A23, A24, A29 |
| Downloads and storage | `DownloadsTest` (core) | R59 (via A25), R60, R61, R85, A25, A39, A43–A45 |
| Home | `HomeTest` (core) | R18–R21, R36, R44, A17 |
| Shell | `ShellUiTest` (Android) | N01, N10, A38 |
| Settings UI | `SettingsUiTest` (Android) | S01 (R50), S02, S03 (A42), S10 (R81) |
| Element IDs | `ResourceIdUiAutomatorTest` (Android) | R69 |
| Home UI | `HomeUiTest` (Android, `UI_HOME_SPEC.md`) | H01–H12: R18–R21, R44, R62, R64, A05, A17, A39 |
| Search UI | `SearchUiTest` (Android, `UI_SEARCH_SPEC.md`) | Q01–Q14: R22–R29, R62, R64, R71, A05, A07, A10, A27, A35 |
| Item and collection UI | `ItemUiTest`, `LibraryUiTest#I15` (Android, `UI_ITEM_SPEC.md`) | I01–I16: R30–R33, R35, R51–R53, R57, R59, R70, A11–A13, A21, A26 |
| Player UI and playback | `PlayerUiTest` (Android, `UI_PLAYER_SPEC.md`) | P01–P18, P20: R34–R45, R64, A13, A15–A17, A32–A34 |
| Library UI | `LibraryUiTest` (Android, `UI_LIBRARY_SPEC.md`) | L01–L16: R35, R44, R50–R58, R61, A21, A23–A25 |
| Flavours | `CastFullUiTest` (full), `NoCastFossUiTest` (foss), `verifyFossDependencies` (Gradle) | R46, R80, R81, A37, N13, P19, L17 |

**Not yet covered by automated tests** (to be added with the matching slice, or checked on a device):

- Manual on a phone: R39 background video, R47/R48/A18–A20 Cast hand-over (real Chromecast), removable SD card (R85), R84 logo assets, tablet/foldable layouts A28, A30, A31.
- GitHub Pages share page itself and App Link domain verification: R56, A22 (the in-app link import, R55, is covered by `LibraryUiTest`).
- Android backup: R73. Performance targets: R77 (emulator with network shaping + phone).
- Scope/architecture statements R01–R06, R16, R79 are checked by review, not tests.

## UI test seams

Android UI tests never call archive.org. `support/FixtureServer` (OkHttp MockWebServer) serves `app/src/androidTest/assets/fixtures/` (generated by `tools/gen_ui_fixtures.py`) and sets `AppEnvironment`: `archiveBaseUrl`, `pagesBaseUrl`, `networkOverride`, `freeBytesOverride` and `shuffleSeed`. Production code reads these at use time. `support/PlayerProbe` connects a Media3 `MediaController` to `playback.PlaybackService` to check queue, position and speed.

## Evidence

Actual commands, counts and results go in TDD_EVIDENCE.md, never here.
