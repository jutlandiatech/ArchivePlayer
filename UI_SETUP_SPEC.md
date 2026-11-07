# First-launch setup

Status: **draft v0.1 for review**, 7 November 2026. Companion to SPECIFICATION.md R27, R65–R69, R70 and decision D15. The layout follows jutlandiatech/sim-warning-app's setup approach, adapted to ArchivePlayer's dark-first theme. Not approved.

## Flow

Setup runs once on first launch, before the main UI. It has three short steps with "Step N of 3" text and progress marks (meaning must not depend on colour alone):

1. **Language** — choose the UI language.
2. **Content languages** — choose preferred content languages (optional).
3. **Ready** — short privacy note and Start.

A primary full-width button ("Continue", on the last step "Start") and, from step 2 on, a Back action. Android Back moves one step back within setup; on step 1 it leaves the app without completing setup. Selections survive back/forward navigation, rotation and activity recreation. Completing setup persists the choices and the "setup completed" flag, then opens Home. After that, launching the app goes straight to Home.

## Step 1 — Language

- English heading: "Choose your language". Supporting text: "You can change this later in Settings."
- One single-selection row per discovered text catalog (R66), each showing the flag and the native language name (Dansk, Deutsch, English). Sorted by native name.
- Preselect the catalog matching the phone's language; otherwise English. Exactly one row is selected.
- Selecting a row updates all setup text and the title immediately, keeping the current step.
- Flags are decorative; the language name is the accessible label.

## Step 2 — Content languages

- English heading: "Which languages do you watch and listen in?". Supporting text: "We'll favour these on Home and offer them as quick filters. You can skip this."
- Multi-select chips for common content languages, shown with native names. The UI language's content language is preselected; English is preselected too when the UI language is not English (SO1). A "More languages" action opens the full list of languages known to the normalization table (R13).
- Zero selections are allowed (no preference).

## Step 3 — Ready

- English heading: "You're all set".
- Privacy note: "No account, no tracking. Your playlists and history stay on your phone."
- Content-filter note: "Adult content is hidden. You can change this in Settings." (R70; the setting is not offered during setup.)
- Primary action "Start" completes setup and opens Home. A rapid double tap completes setup only once.

## Text catalog contract

All setup text comes from the TextCatalog files (R65). Initial keys:

| Key | English |
|---|---|
| `app.name` | ArchivePlayer |
| `language.self` | English (each catalog: its own native name) |
| `setup.progress` | Step {current} of {total} |
| `setup.language.title` | Choose your language |
| `setup.language.hint` | You can change this later in Settings. |
| `setup.contentLanguages.title` | Which languages do you watch and listen in? |
| `setup.contentLanguages.hint` | We'll favour these on Home and offer them as quick filters. You can skip this. |
| `setup.contentLanguages.more` | More languages |
| `setup.ready.title` | You're all set |
| `setup.ready.privacy` | No account, no tracking. Your playlists and history stay on your phone. |
| `setup.ready.adultContent` | Adult content is hidden. You can change this in Settings. |
| `action.continue` | Continue |
| `action.back` | Back |
| `action.start` | Start |

Danish and German texts are drafted in the catalog files during the test gate and reviewed by Thomas.

## Element IDs

| ID | Element |
|---|---|
| `setup_root` | Setup root |
| `setup_progress` | Progress text and marks |
| `setup_language_screen` | Step 1 root |
| `setup_language_title` / `setup_language_hint` | Heading / hint |
| `setup_language_<code>` | Language row per catalog, e.g. `setup_language_en`, `setup_language_de`, `setup_language_da`; selected state exposed |
| `setup_content_languages_screen` | Step 2 root |
| `setup_content_languages_title` / `setup_content_languages_hint` | Heading / hint |
| `setup_content_language_<iso>` | Content-language chip, e.g. `setup_content_language_fr`; checked state exposed |
| `setup_content_languages_more_button` | Opens the full language list |
| `setup_ready_screen` | Step 3 root |
| `setup_ready_title` / `setup_ready_privacy` / `setup_ready_adult_content` | Ready texts |
| `setup_continue_button` | Continue on steps 1–2 |
| `setup_back_button` | Back on steps 2–3 |
| `setup_start_button` | Start on step 3 |

## Acceptance scenarios

| ID | Scenario and expected result |
|---|---|
| U01 | First launch with phone language da, de, en, and an unsupported language (es): correct preselection (es → English); exactly one row selected; "Step 1 of 3". |
| U02 | Select each language row by ID: setup text changes immediately; IDs and selected state stay stable. |
| U03 | Continue through steps 1–3: exactly one step root visible at a time; progress increments; Back absent on step 1, present on steps 2–3. |
| U04 | In-app Back and Android Back on steps 2–3 return one step and keep selections; Android Back on step 1 leaves the app and setup is shown again on next launch. |
| U05 | Content languages: default preselection per UI language; toggling chips by ID; zero selections allowed; "More languages" adds a language. |
| U06 | Rotate / recreate the activity on each step: same step and selections. |
| U07 | Start: Home opens; UI language and content languages persisted; relaunch opens Home directly. Double tap on Start completes once. |
| U08 | Adding a test-only fourth catalog in the test APK: it appears as a language row without code changes. |
| U09 | All languages at normal and 200% font scale on 320 dp width: no clipping; actions reachable by scrolling; touch targets ≥ 48 dp. |
| U10 | TalkBack: headings, single/multi selection states, button labels; flags not announced redundantly. |

## Open points

| ID | Topic | Proposed default |
|---|---|---|
| SO1 | Content-language preselection | UI language's content language, plus English when the UI language isn't English. |
| SO2 | Content-language chip list | English, German, Danish, French, Spanish, Italian, Swedish, Norwegian, Dutch, Japanese; the rest under "More languages". |
