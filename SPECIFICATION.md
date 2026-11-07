# ArchivePlayer — requirements and design specification

Version: 0.1 (draft for review), 7 November 2026.

Status: **draft — not yet approved.** Requirement rows record the 21 product decisions Thomas made during the requirements interview. Everything listed under *Open points* (O01…) is a proposed default and is **not approved behavior** until accepted in review. Per the SpecAndTestDrivenDevelopment workflow, no tests or production code are written against this document until it is approved; tests must then be written before the implementation they cover.

Companion documents:

- [UI_SHELL_SPEC.md](UI_SHELL_SPEC.md) — navigation shell, mini-player, theme, element-ID conventions.
- [UI_SETUP_SPEC.md](UI_SETUP_SPEC.md) — first-launch setup (language, content languages, adult-content default).
- Further `UI_<AREA>_SPEC.md` files (Home, Search, Item, Player, Library, Playlist, Settings) follow after this document is approved.
- `TESTING.md` and `TDD_EVIDENCE.md` are created in the test gate.

## 1. Purpose and scope

| ID | Requirement |
|---|---|
| R01 | ArchivePlayer makes the audio and video hosted on the Internet Archive (archive.org) convenient to discover, stream on the phone, and cast to a Chromecast device. |
| R02 | Native Android app in Kotlin with Jetpack Compose (Material 3) and Jetpack Media3 (ExoPlayer, MediaSession, Cast). Android 8.0 / API 26 and newer. |
| R03 | Open-source project on GitHub under the Apache License 2.0. Not targeted at Google Play; distributed as APK (F-Droid compatibility is desirable, see O20). |
| R04 | v1 is anonymous and local-only: no sign-in, no ArchivePlayer server, no analytics, no advertising, no tracking. The app contacts only archive.org and the project's GitHub Pages site (R15, R54–R56). |
| R05 | Design the data model so optional archive.org sign-in (e.g. syncing playlists to archive.org Lists) can be added later without migrating away from local identifiers. Sign-in is out of scope for v1. |
| R06 | Supported form factors: phones (portrait and landscape) and tablets/foldables with dedicated large-screen layouts (R62). Android TV and Android Auto apps are out of scope; standard media-session controls (headsets, Bluetooth, Wear/Auto media controls provided by the system) must still work. |

## 2. Content source and catalog model

archive.org facts that shape these requirements (observed November 2026; counts are time-dependent): roughly 17.6M `movies` items, 14.3M `audio` items, 0.3M `etree` live-concert items. `subject` is free text and noisy; `genre` is sparse; curated collections are the cleanest taxonomy; `language` uses mixed values (`fre`, `French`, `fr`, `Français`) and is missing on many items; playable derivatives are progressive files (h.264 MP4, "512Kb MPEG4", VBR MP3, Ogg Vorbis, FLAC); download URLs redirect to `dn*.ca.archive.org`, support HTTP byte ranges and send `Access-Control-Allow-Origin: *`.

| ID | Requirement |
|---|---|
| R07 | Media kinds: treat archive.org `mediatype` `movies` as **video** and `audio` and `etree` as **audio**. Other media types are excluded. |
| R08 | Model content in three levels: **Collection** (archive.org collection or creator), **Item** (archive.org item), **Entry** (one playable file/track/episode inside an item). Single-file items have exactly one entry. |
| R09 | Show only playable content. An item is playable when it contains at least one entry in a format supported for both local playback and Cast (R40). Search queries pre-filter by format where the API allows; items that turn out to have no playable entry when opened show a clear "nothing playable" state and are excluded from rows, playlists and downloads. |
| R10 | Entries are grouped from archive.org files: original and derivative files of the same track/episode form one entry with several available formats. Non-media files (images, text, spectrograms, metadata, torrents) are not entries; images may be used as artwork; subtitle files attach to their entry (R45). |
| R11 | Entry order follows the item's track metadata (`track` field) when present, otherwise natural filename order. |
| R12 | Display metadata per item: title, creator, date/year, duration, media kind, normalized languages, collection(s), description (HTML stripped/sanitized), artwork (item image or first image file, with a generated placeholder fallback), archive.org link. |
| R13 | Language normalization: map archive.org `language` values (ISO 639-1/639-2 codes, English names, native names, case variants) to ISO 639-1 codes through a versioned mapping table covered by unit tests. Unmapped values are ignored for filtering and logged in debug builds. Items without any mappable language are "untagged". |

## 3. Curated category map

| ID | Requirement |
|---|---|
| R14 | Discovery categories come from a hand-curated, versioned **category map** (JSON). Each category has a stable ID, localized names (English fallback), media kind, optional artwork, child categories, and a query definition composed of archive.org collections, subjects/genres and other field filters. Example: Video → Movies → Film Noir = collection `Film_Noir` OR subject `film noir`. |
| R15 | A copy of the category map is bundled in the APK. The app fetches the latest version from the project's GitHub Pages site, validates it against the schema version it supports, caches it, and falls back to the cached or bundled copy when the fetch fails or validation fails. Never show a broken or partially parsed map. |
| R16 | The category map file lives in the repository and is updated via normal pull requests; changing categories never requires a new APK as long as the schema version is unchanged. |
| R17 | The initial map covers at least: Video — Movies (by genre, e.g. Comedy, Drama, Film Noir, Sci-Fi/Horror, Silent, Animation), TV (classic TV by decade), Documentaries; Audio — Music (by genre, e.g. Rock, Pop, Jazz, Classical, Folk/World, Hip-hop, 78rpm records, Netlabels), Live concerts (etree, popular artist collections), Old Time Radio, Podcasts, Spoken word / Audiobooks. Exact list: O01. |

## 4. Home screen

| ID | Requirement |
|---|---|
| R18 | Home is a vertical list of horizontal rows (Spotify-style). Personal rows: Continue watching/listening, Recently played, Your playlists, Downloads, New in collections you follow. Curated rows come from the category map. Personal rows are hidden when empty. |
| R19 | "Because you played X" rows are computed on the device from local history (same collection, creator, or subject as recently completed or long-played items). No history leaves the device. |
| R20 | Curated rows favour the user's preferred content languages (R27) without hiding content that has no language tag. |
| R21 | "New in collections you follow" is refreshed when the app opens (and on pull-to-refresh), at most once per O09 interval, showing items added since the last check. No push notifications in v1. |

## 5. Search and browse

| ID | Requirement |
|---|---|
| R22 | Search tab: query field with as-you-type suggestions drawn from local search history, followed collections and category names; remote suggestions only if archive.org offers a suitable endpoint (O10). Suggestion requests are debounced (O11). |
| R23 | Filters (chips): media kind (all/audio/video), category, language, decade, duration class (short < 20 min, episode 20–60 min, feature > 60 min — O12). Filters combine with AND; selected filters remain visible and individually removable. |
| R24 | Sort: relevance (default), popularity (archive.org download counts), newest added, original date. |
| R25 | Results are grouped: Collections, Items, and the user's own playlists matching the query. Results page incrementally (infinite scroll) and show a clear empty state and error/retry state. |
| R26 | Without a query, the Search tab shows the category map as browsable tiles (browse). Opening a category shows its sub-categories and a results list using the same filters and sorts. |
| R27 | Preferred content languages: chosen during setup (UI_SETUP_SPEC.md) and in Settings; zero or more ISO languages. |
| R28 | When a language filter is active, only items tagged with a matching normalized language are shown; untagged items are excluded. Without a language filter, untagged items are shown normally. |
| R29 | Recent searches are stored locally, shown in the Search tab, and can be removed individually or cleared. |

## 6. Item pages

| ID | Requirement |
|---|---|
| R30 | Every item opens as an album/show page: artwork, title, creator, year, media kind, languages, collection link, Like, Follow (collection/creator), Share, Download (audio only, R59), Play all, Shuffle, and the list of entries. Single-entry items use the same page with one entry. |
| R31 | Entries can be played individually (starting playback of the item from that entry), added to the queue (play next / add to queue), added to a playlist, liked, and (audio) downloaded. The whole item can be added to a playlist as its entries. |
| R32 | Long entry lists (hundreds of episodes) are virtualized and searchable/filterable within the page (O13). |
| R33 | Collection pages show collection metadata, Follow, and the collection's items with the same filters/sorts as search. |

## 7. Playback

| ID | Requirement |
|---|---|
| R34 | One shared Media3 player behind a `MediaSessionService` provides background playback, notification and lock-screen controls, audio focus handling, and becoming-noisy (headphones unplugged) pause. |
| R35 | Queue: playing an item, playlist or Liked list replaces the queue in order; users can play next, add to queue, reorder and remove queue entries. Audio and video entries may be mixed in one queue; the player switches between the audio layout (artwork) and the video surface per entry. |
| R36 | Resume position is stored per entry (when more than O14 seconds played and not within the last O14 seconds) and feeds Continue watching/listening. Finished entries are marked played. |
| R37 | Controls: play/pause, seek bar, previous/next, skip back/forward (10 s / 30 s, O15), playback speed 0.5–2.0×, shuffle, repeat off/all/one, sleep timer (minutes or end of current entry). |
| R38 | Picture-in-picture for video when the user leaves the app during video playback (Android 8+), with play/pause and skip actions. |
| R39 | Background audio continues with the screen off. When a video entry plays in the background without PiP, its audio continues (O16). |
| R40 | Format selection: automatically pick the best format playable both locally and on the default Cast receiver — video: h.264 MP4 (preferred over "512Kb MPEG4" and others); audio: VBR MP3, then 128 kbps MP3, then Ogg Vorbis (local only, O17). FLAC is never auto-selected in v1. |
| R41 | Data saver setting: on metered networks prefer the lower-bitrate video derivative ("512Kb MPEG4") and the lowest-bitrate MP3. Default: on (O18). |
| R42 | Quality menu in the player lists the entry's alternative playable formats; a manual choice applies to the current entry only. |
| R43 | Playback errors (network loss, HTTP errors, unsupported format) show a recoverable error with Retry and automatically fall back to the next suitable format once before reporting. |
| R44 | Played entries and play events are recorded locally in history (for R18/R19), and history recording can be paused and cleared in Settings. |
| R45 | Subtitles: when an entry has SubRip (`.srt`) or WebVTT (`.vtt`) files, offer them in a subtitle menu (off by default). archive.org also generates machine transcripts for many items; label such tracks distinctly when identifiable (O19). |

## 8. Chromecast

| ID | Requirement |
|---|---|
| R46 | Full Cast support with Google's Default Media Receiver: a Cast button in the top bar and player; any entry, item, playlist or queue can be cast. |
| R47 | The whole queue is handed to the receiver so playback continues when the phone is locked, the app is backgrounded, or its process is killed. |
| R48 | Playback transfers seamlessly between phone and Chromecast in both directions, preserving queue, current entry and position. |
| R49 | While casting, the app, notification and lock screen control the Cast session (play/pause, seek, next/previous, volume). Cast-only limitations (e.g. picture-in-picture, local subtitles if unsupported) are handled gracefully. Formats sent to Cast must satisfy R40. |

## 9. Library: playlists, likes, follows, history

| ID | Requirement |
|---|---|
| R50 | Library tab with filter chips: Playlists, Liked, Following, Downloads, History. Settings is reachable from the Library top bar. |
| R51 | Playlists: create, rename, describe, delete (with undo), reorder entries, remove entries; mixed audio and video entries allowed; artwork is a mosaic of the first entries' artwork. Playlists can be played, shuffled, cast, shared, exported and (audio entries) downloaded. |
| R52 | Liked: a heart on any entry or item adds it to an automatic "Liked" list that behaves like a playlist (play, shuffle, cast, share, download) but cannot be renamed or deleted. |
| R53 | Following: users follow collections and creators; followed sources feed the Home row (R21) and are listed in Library. |

## 10. Sharing and export

| ID | Requirement |
|---|---|
| R54 | Share link: a playlist (or the Liked list, or a single item/entry) can be shared as an https link to the project's GitHub Pages site. The link fragment encodes a versioned, compressed payload (title, optional description, ordered archive.org identifiers and file names); no data is sent to any server when creating or opening the link. |
| R55 | With ArchivePlayer installed, opening a share link opens an import preview in the app (verified Android App Link where possible, O05). Importing creates a new local playlist; it never overwrites an existing one. |
| R56 | Without the app, the GitHub Pages share page renders the playlist from the fragment: titles (fetched from archive.org by the page), links to archive.org, and a link to the app's download page. |
| R57 | Very long playlists whose link would exceed O06 characters are shared as a file instead, with a clear explanation. |
| R58 | Playlist file export/import via the Android share sheet and Storage Access Framework, format `.archiveplaylist` (JSON, versioned schema). The full-library export (R66) is a superset of this format. Import validates the schema and reports skipped/invalid entries. |

## 11. Offline downloads (audio only)

| ID | Requirement |
|---|---|
| R59 | Audio entries, whole audio items, playlists and Liked can be downloaded for offline playback. Video entries are not downloadable in v1; when downloading a mixed playlist, only its audio entries download and the UI says so. |
| R60 | Downloads use a background download manager (Media3 `DownloadService`) that survives app closure, resumes after network loss, and respects a "download on Wi-Fi only" setting (default on, O07). |
| R61 | Downloaded entries play offline automatically (cast falls back to streaming). Storage use is shown; downloads can be removed individually or all at once. Optional storage limit: O08. |

## 12. Devices, layout, look and feel

| ID | Requirement |
|---|---|
| R62 | Large screens (≥ 600 dp width) use tablet-optimized layouts: navigation rail instead of bottom bar, two-pane list/detail where useful, and the expanded player beside content. See UI_SHELL_SPEC.md. |
| R63 | Dark-first visual style: dark theme by default; light theme when the system is set to light (follow system). One fixed brand accent colour (proposed in O02); no dynamic (wallpaper) colour. |
| R64 | Accessibility: Android text scaling, TalkBack labels for all controls, touch targets ≥ 48 dp, sufficient contrast in both themes. Validate all three UI languages at enlarged font sizes without clipping. |

## 13. Localization and stable element IDs

| ID | Requirement |
|---|---|
| R65 | UI languages: English, German, Danish. All app-owned text comes from per-language JSON text catalogs `app/src/main/assets/texts/TextCatalog_<CODE>.json` (`ENG`, `GER`, `DEN`), using stable text keys and named `{placeholders}` with strict argument checking, following the approach of jutlandiatech/sim-warning-app. |
| R66 | Adding a UI language requires only adding a new catalog file: the app discovers catalogs in `assets/texts/`, and each catalog names itself (`language.self`, plus its ISO code). A unit test fails the build if any catalog's key set differs from the English reference or contains blank values. |
| R67 | First launch has a setup step that selects the UI language: rows with flag and native language name, preselecting the phone's language when a catalog exists, otherwise English; switching updates the UI immediately. The choice can be changed later in Settings. |
| R68 | Category names in the category map carry translations; missing translations fall back to English. Content metadata from archive.org is shown as-is (not translated). |
| R69 | Every app-owned screen root, interactive control and status element has a stable, language-independent element ID (Compose `testTag`, exposed as resource IDs via `testTagsAsResourceId`) registered in the UI specs. Tests locate elements by ID, never by localized text or coordinates; they separately assert visible text against catalog values. |

## 14. Content filtering

| ID | Requirement |
|---|---|
| R70 | Adult content is hidden by default. Settings offers an opt-in "Show adult content" switch with a short explanation. |
| R71 | Filtering is best-effort and must be stated as such in Settings/Help. Proposed mechanism (O03): exclude archive.org items in known adult/no-preview collections and with adult subject keywords, applied both to search queries and as a client-side check on item pages, playlists imported via link/file, and Home rows. |

## 15. Settings, backup and privacy

| ID | Requirement |
|---|---|
| R72 | Settings: UI language, preferred content languages, data saver, download on Wi-Fi only, storage/downloads, subtitles default, show adult content, history (pause/clear), clear search history, export/import library, about (version, license, open-source notices, archive.org attribution). |
| R73 | Library data (playlists, likes, follows, history, settings) is stored locally (Room/DataStore) and included in Android Auto Backup. Downloaded media and caches are excluded from backup. |
| R74 | "Export library" / "Import library" writes/reads one versioned JSON file via the Storage Access Framework, working without Google services. Import merges (no silent overwrite) and reports conflicts. |
| R75 | No analytics, crash reporting SDKs, or third-party trackers. Network access is limited to archive.org domains and the project's GitHub Pages domain. |

## 16. Non-functional requirements

| ID | Requirement |
|---|---|
| R76 | Be a good archive.org citizen: descriptive User-Agent with app name, version and project URL; HTTP caching of search and metadata responses; request debouncing; paging rather than bulk crawling; exponential back-off on HTTP 429/5xx. No bulk or background crawling. |
| R77 | Responsiveness: Home renders cached content immediately and refreshes in the background; the UI never blocks on network. Targets (O04): first Home content < 2 s on a warm cache, search results < 2 s on a typical 4G connection. |
| R78 | Robustness: archive.org metadata is untrusted input. Parsing tolerates missing/odd fields and never crashes the app; descriptions are sanitized before display. |
| R79 | Architecture separates a pure-Kotlin core (catalog model, language normalization, category-map parsing, format selection, share-link encoding, playlist/queue logic) from Android adapters, so most behavior is unit-testable on the JVM with MockWebServer-based functional tests. |

## 17. Acceptance scenarios

These scenarios define intended behavior, not completed tests. Each must be translated into tests before its production implementation.

| ID | Given / when | Expected outcome | Requirements |
|---|---|---|---|
| A01 | First launch on a German phone | Setup preselects Deutsch; UI shows German catalog text | R65–R67 |
| A02 | First launch on a phone set to Spanish | Setup preselects English | R67 |
| A03 | A new `TextCatalog_FRA.json` with all keys is added to assets | French appears as UI language choice without code changes | R66 |
| A04 | A catalog misses one key | Catalog-parity test fails | R66 |
| A05 | Item has `language` = `fre`, another `French`, another `Français` | All three match the French filter and display as French | R13, R28 |
| A06 | Language filter French is active; a film has no language field | Film is not in results | R28 |
| A07 | No language filter; same film | Film can appear in results | R28 |
| A08 | Remote category map is unreachable on first start | Bundled map is used; categories are shown | R15 |
| A09 | Remote map has an unsupported schema version or invalid JSON | Previous cached or bundled map is used | R15 |
| A10 | Search "gunsmoke", filter audio, sort popularity | Audio results sorted by download count; grouped Collections / Items / Playlists | R22–R25 |
| A11 | Open an item with 487 episodes | Album/show page with virtualized list; entry order per R11 | R30–R32 |
| A12 | Item with only images and text | Not shown in results; if opened by link, shows "nothing playable" | R09 |
| A13 | Video entry offers h.264 and 512Kb MPEG4, data saver on, metered network | 512Kb MPEG4 selected; quality menu lists both | R40–R42 |
| A14 | Same entry on Wi-Fi | h.264 selected | R40–R41 |
| A15 | Audio entry offers VBR MP3, 128 kbps MP3, Ogg, FLAC | VBR MP3 selected; FLAC listed but never auto-selected | R40 |
| A16 | Play a mixed playlist (audio, video, audio) | Player switches layout per entry; queue order kept | R35 |
| A17 | Stop a 90-minute film at 40 min, reopen app | Film in Continue watching; resumes at 40 min | R36 |
| A18 | Start Cast during playback of entry 3 of a playlist at 12:34 | Playback continues on Chromecast at entry 3, 12:34 with full queue | R46–R48 |
| A19 | While casting, lock phone and kill the app process | Receiver continues through the queue | R47 |
| A20 | Stop Cast session | Playback continues on the phone at the current position | R48 |
| A21 | Share a 20-entry playlist; open link on a phone with the app | Import preview; importing creates a new playlist with the same 20 entries in order | R54–R55 |
| A22 | Open the same link in a desktop browser | Pages site lists the 20 entries with archive.org links | R56 |
| A23 | Share a playlist whose link exceeds the limit | User is offered file sharing instead | R57 |
| A24 | Export a playlist file and import it on another phone | Identical playlist created; invalid entries reported | R58 |
| A25 | Download a mixed playlist on mobile data with Wi-Fi-only on | Only audio entries queued; downloads wait for Wi-Fi; UI explains both | R59–R60 |
| A26 | Airplane mode; play a downloaded audio entry | Plays offline | R61 |
| A27 | Adult setting off; search a term matching known adult items | Those items are not shown | R70–R71 |
| A28 | Like an entry; follow a collection; restart app | Entry in Liked; collection in Following and feeds Home row | R52–R53, R21 |
| A29 | Export library, clear app data, import library | Playlists, likes, follows, settings restored | R74 |
| A30 | Tablet in landscape | Navigation rail and two-pane layouts | R62 |
| A31 | All three languages at 200% font scale, compact phone width | No clipping; controls reachable; element IDs unchanged | R64, R69 |
| A32 | Play video, press Home | Picture-in-picture with controls | R38 |
| A33 | Entry has `.srt` subtitles | Subtitle menu offers it; off by default | R45 |
| A34 | Sleep timer "end of current entry" | Playback pauses when the entry ends | R37 |
| A35 | archive.org returns HTTP 429 | Back-off and user-visible retry; no request storm | R76 |
| A36 | Run the setup scenarios in UI_SETUP_SPEC.md and shell scenarios in UI_SHELL_SPEC.md | Stable IDs, correct navigation, localization and accessibility | R62–R69 |

## 18. Open points (proposed defaults, not approved)

| ID | Topic | Proposed default |
|---|---|---|
| O01 | Initial category list | The categories listed in R17, each mapped to concrete collections/subjects in the first category-map file, reviewed with sample results before approval. |
| O02 | Brand accent colour | Warm archive amber (around `#F2A33A`) on near-black (`#121212`) surfaces; alternative: Nordic blue `#2769AD` matching sim-warning-app. |
| O03 | Adult-content detection | Exclude `collection:no-preview` (about 60k items) and items whose subjects match a curated keyword list (adult, porn, xxx, nsfw, erotic, …); keep the list in the category-map repo so it can be updated without a release. |
| O04 | Performance targets | As in R77; measured on an emulator with network shaping and on Thomas's phone. |
| O05 | Share-link hosting | GitHub Pages at `https://jutlandiatech.github.io/ArchivePlayer/` with `/.well-known/assetlinks.json` for verified App Links. The repository is currently private: Pages on a private repo requires a paid GitHub plan, so the repo should be made public before sharing is released. |
| O06 | Maximum share-link length | 8,000 characters (≈ 300–500 entries after compression); larger playlists use file sharing. |
| O07 | Download on Wi-Fi only | Default on. |
| O08 | Download storage limit | No hard limit in v1; show used space and free space; warn below 500 MB free. |
| O09 | "New in followed" refresh interval | At most every 6 hours, on app open or pull-to-refresh. |
| O10 | Remote search suggestions | None in v1 unless a stable archive.org suggestion endpoint is confirmed; local suggestions only. |
| O11 | Suggestion debounce | 300 ms. |
| O12 | Duration classes | Short < 20 min, episode 20–60 min, feature > 60 min. |
| O13 | In-item entry search | Filter field shown for items with more than 30 entries. |
| O14 | Resume thresholds | Save position after 30 s; treat the last 30 s (or 95%) as finished. |
| O15 | Skip intervals | Back 10 s, forward 30 s. |
| O16 | Video in background without PiP | Audio continues; video resumes on return. |
| O17 | Ogg Vorbis on Cast | Not sent to Cast; MP3 used instead. |
| O18 | Data saver default | On, applies to metered networks only. |
| O19 | Machine-generated subtitles | Show them, labelled "Auto-generated" when the file name or metadata identifies them. |
| O20 | F-Droid | Keep dependencies FOSS-compatible (no Google Play Services beyond the Cast framework); evaluate F-Droid packaging after v1, noting the Cast SDK is proprietary. |
| O21 | Setup flow content | Language → preferred content languages → short privacy note; adult-content default stays off and is not asked during setup (see UI_SETUP_SPEC.md). |

## 19. Decision log

| # | Decision (7 November 2026) | Requirements |
|---|---|---|
| D01 | Anonymous, local-only v1; sign-in possible later | R04–R05 |
| D02 | Share via GitHub Pages link plus file export/import | R54–R58 |
| D03 | Items open as album/show pages with selectable entries | R30–R32 |
| D04 | Playlists may mix audio and video | R35, R51 |
| D05 | Adult content hidden by default, opt-in to show | R70–R71 |
| D06 | Offline downloads in v1, audio only | R59–R61 |
| D07 | Phone plus tablet-optimized layouts | R06, R62 |
| D08 | Full Cast with the Default Media Receiver | R46–R49 |
| D09 | Category map as remote JSON on GitHub Pages with bundled copy | R14–R16 |
| D10 | Preferred content languages plus filter; untagged excluded when filtering | R27–R28 |
| D11 | Automatic format with manual override and data saver | R40–R42 |
| D12 | Home: personal rows, curated rows, on-device "Because you played" | R18–R21 |
| D13 | Full search: suggestions, filters, sorts, grouped results | R22–R29 |
| D14 | UI in English, German, Danish via TextCatalog JSON files and stable element IDs | R65–R69 |
| D15 | UI-language setup as in sim-warning-app | R67 |
| D16 | Apache-2.0 license | R03 |
| D17 | Navigation: Home, Search (incl. browse), Library, plus mini-player | UI_SHELL_SPEC.md |
| D18 | Dark-first, fixed brand accent, light follows system | R63 |
| D19 | Rich player incl. subtitles and picture-in-picture | R34–R45 |
| D20 | Likes plus followed collections/creators with "New" row | R21, R52–R53 |
| D21 | Android backup plus manual full export/import; no analytics | R73–R75 |
