# Data and integrations

Implementation reviewed 15 November 2026. See [UML](ARCHITECTURE_UML.md) for runtime relationships.

## Local persistence

| Data | Implementation |
|---|---|
| Settings/setup | `SettingsStore`: private SharedPreferences named `settings`; load returns null until `setupDone`. |
| Library | `LibraryStore`: `filesDir/library.json`; playlists, likes, follows, history, recent queries and last follow check. |
| Download index | `Downloads`: `filesDir/downloads.json`; references, selected files, URLs, locations, states and byte counters. |
| Audio bytes | Internal `filesDir/downloads` or removable app-specific `downloads` directory; pending transfers use `.part`. |
| Category map | Bundled asset with optional validated `filesDir/category-map.json` cache selected on a later launch. |
| Adult keywords | Bundled `assets/catalog/adult-keywords.json`; no repository refresh path. |
| Metadata cache | In-memory `ConcurrentHashMap`; not a persistent offline catalog. |
| Interface text | Bundled ENG, GER and DEN JSON catalogs in `assets/texts`. |
| Shared files | Temporary `cacheDir/shared`, exposed via FileProvider read grants. |

History updates cap records at 500; submitted recent searches cap at 20. Library/download decoding falls back to empty/default state on unreadable data. This is not a data migration or corruption-repair system. Library writes are direct file writes; download-index write exceptions are caught. These stores are not transactional databases.

Sources: [LibraryStore](../app/src/main/java/com/jutlandiatech/archiveplayer/data/LibraryStore.kt), [SettingsStore](../app/src/main/java/com/jutlandiatech/archiveplayer/data/SettingsStore.kt), [Downloads](../app/src/main/java/com/jutlandiatech/archiveplayer/data/Downloads.kt).

## External requests

| Purpose | Boundary |
|---|---|
| Website-ranked search | Archive `services/search/beta/page_production` when applicable; failure falls back to advanced search. |
| Advanced search | Archive `advancedsearch.php`, built from `SearchRequest`. |
| Metadata | Archive `/metadata/{identifier}`. |
| Media | Archive `/download/{identifier}/{encoded file path}`. |
| Images | Archive `/services/img/{identifier}`. |
| Category map | `https://jutlandiatech.github.io/ArchivePlayer/catalog/category-map.json`. |
| Playlist links | GitHub Pages `/ArchivePlayer/p` with versioned compressed fragment payload. |

`ArchiveClient` adds a version User-Agent, checks the initial host against Archive/GitHub Pages/test hosts, and retries 429/5xx up to three total attempts. It uses integer-second Retry-After or exponential delays. Downloads and Media3 use other network paths; this client is not a universal network gateway or redirect-validation layer.

Category refresh validates JSON before writing a temporary cache and renaming it. It does not replace an already selected in-memory map. Search results live in controller state; item metadata is cached by the repository in memory.

Sources: [repository](../app/src/main/java/com/jutlandiatech/archiveplayer/data/ArchiveRepository.kt), [network client](../core/src/main/kotlin/com/jutlandiatech/archiveplayer/core/net/ArchiveClient.kt), [search builders](../core/src/main/kotlin/com/jutlandiatech/archiveplayer/core/search/Search.kt).

## Import, export and Android integration

Pure Kotlin [playlist/library codecs](../core/src/main/kotlin/com/jutlandiatech/archiveplayer/core/playlist/Playlists.kt) handle serialization. `AppViewModel` connects them to intents, previews, share choosers and document streams. Library export includes playlists, likes, follows, history and settings, but neither media bytes nor download index. Library import uses `LibraryMerger`; it does not reconstruct downloaded files.

`MainActivity` is exported for launch/import and `PlaybackService` for Media3/external controllers. FileProvider and download keep-alive service are not exported. Backup is enabled with internal/external download folders excluded by [backup_rules.xml](../app/src/main/res/xml/backup_rules.xml) and [data_extraction_rules.xml](../app/src/main/res/xml/data_extraction_rules.xml). Actual restoration still needs device validation.

[AppEnvironment](../app/src/main/java/com/jutlandiatech/archiveplayer/AppEnvironment.kt) provides instrumentation overrides for URLs, connectivity, storage, shuffle and Cast state. These are test seams rather than user configuration. Debug cleartext settings are separate from release configuration.
