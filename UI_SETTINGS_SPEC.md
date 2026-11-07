# Settings design and automation contract

Status: **draft v0.2 for final review**, 7 November 2026. Implements SPECIFICATION.md R27, R44–R45, R50, R60–R61, R67, R70, R72–R74, R82–R83, R85. Reference: [interactive mockup](design/archiveplayer-mockup.html) → gear icon. Not yet approved.

## Layout and navigation

Nordic light/noir style (R63, R82), 18sp body text, about 28sp screen heading, touch targets ≥ 48dp. Settings opens from the gear action in the top bar of every tab root and is pushed onto the current tab's back stack.

The main list is a column of full-width rounded cards (R83): blue leading icon, label, current value as secondary text, trailing chevron. Icons supplement text, never replace it. Order:

1. App language — native name of the selected UI language
2. Appearance — Nordic light / Nordic noir / Follow system
3. Content languages (watch & listen) — selected languages, or "—"
4. Data saver — On/Off
5. Download on Wi-Fi only — On/Off
6. Show adult content — On/Off
7. Storage & downloads — "{used} used · {free} free" of the current location
8. Subtitles — default subtitle behaviour
9. History — Recording/Paused
10. Search history — number of stored searches
11. Export / import library
12. About — version and flavour

Each row opens its own detail screen. Changes apply immediately (no Save). In-app Back and Android Back return to the list, then to the tab.

### Detail screens

- **App language:** single-select rows with flag and native name, one per TextCatalog file. Switches all app text immediately; never changes content languages (R27, A42).
- **Appearance:** Nordic light (default), Nordic noir, Follow system; kept in sync with the top-bar toggle (R82).
- **Content languages:** explanation text, then multi-select chips with native names plus "More languages"; never changes the app language.
- **Data saver / Wi-Fi only / Adult content:** one switch each with an explanation. Adult content states that filtering is best-effort (R70). Wi-Fi only mentions the per-download "Download now on mobile data" action (R60).
- **Storage & downloads:** "Download location" (Internal storage / SD card with free space; SD card disabled with "No SD card inserted" when absent), move prompt after a location change, uninstall note, used/free bar, 500 MB warning text, list of downloaded items with location and delete, "Delete all downloads" (R61, R85).
- **Subtitles:** Off (default) / Show when available in my content languages / Always show when available, plus "Include auto-generated subtitles" switch (R45).
- **History:** explanation that history stays on the phone, "Pause history" switch, "Clear history" with confirmation (R44).
- **Search history:** list of recent searches with remove buttons, "Clear all".
- **Export / import library:** Export and Import buttons using the Storage Access Framework, note about Android backup (R73, R74).
- **About:** logo and wordmark, version, build flavour, license (Apache-2.0), open-source notices, GitHub link, archive.org attribution and non-affiliation note.

## Stable automation IDs

Same `testTag` / resource-ID approach as UI_SHELL_SPEC.md. Never select controls by localized label or coordinates.

| ID | Element |
|---|---|
| `top_bar_settings_button` | Gear action on tab roots |
| `settings_screen` | Settings list root |
| `settings_back_button` | Back action (list and details) |
| `settings_row_<name>` | Menu rows: `app_language`, `appearance`, `content_languages`, `data_saver`, `wifi_only`, `adult_content`, `storage`, `subtitles`, `history`, `search_history`, `export_import`, `about` |
| `settings_language_<code>` | App-language rows (`da`, `de`, `en`, …) |
| `settings_appearance_<light\|noir\|system>` | Appearance options |
| `settings_content_language_<code>` | Content-language chips |
| `settings_<data_saver\|wifi_only\|adult_content>_switch` | Switches |
| `settings_storage_location_<int\|sd>` | Download-location options |
| `settings_storage_move_prompt` / `_move_button` / `_keep_button` | Move prompt after location change |
| `settings_storage_delete_<identifier>` / `settings_storage_delete_all` | Delete actions |
| `settings_subtitles_<off\|subLang\|subAlways>` / `settings_subtitles_auto_switch` | Subtitle defaults |
| `settings_history_pause_switch` / `settings_history_clear_button` | History |
| `settings_search_history_remove_<index>` / `settings_search_history_clear_button` | Search history |
| `settings_export_button` / `settings_import_button` | Library export/import |
| `settings_about_notices` / `settings_about_source` | About links |

## Acceptance scenarios

| ID | Scenario and expected result |
|---|---|
| S01 | Open Settings from each tab root: 12 rows in the order above with current values; all IDs discoverable. |
| S02 | Open every detail; app Back and Android Back return to the list, then to the originating tab; selections kept. |
| S03 | Change app language: all text switches immediately and persists; content languages unchanged (A42). |
| S04 | Change content languages: Home/search preferences update; app language unchanged. |
| S05 | Toggle appearance in Settings and via the top bar: both stay in sync and persist across relaunch. |
| S06 | Storage: location switching, move prompt, SD card absent/removed states and deletes behave per R85/A43–A45. |
| S07 | Subtitle defaults apply to the next video with subtitles; auto-generated ones hidden when the switch is off. |
| S08 | Pause history: playing items adds nothing to history/Continue rows; Clear history empties them after confirmation. |
| S09 | Export then import on a clean install restores playlists, likes, follows and settings; downloads are not included. |
| S10 | About shows the correct flavour (`full`/`foss`) and opens notices and the GitHub link. |
| S11 | All three languages, large font, phone and tablet: no clipping, all controls reachable, IDs unchanged. |
