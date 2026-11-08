# Item and collection pages

Status: **approved** (gate 2), 8 November 2026. Implements SPECIFICATION.md R09–R12, R30–R33, R40, R45, R50–R53, R59, R62, R70. Reference: [interactive mockup](design/archiveplayer-mockup.html) → item page.

## Item page layout

Opened from any tab; it lives in that tab's back stack (UI_SHELL_SPEC.md).

- Top bar: back (`item_back_button`), overflow menu (Open on archive.org, Copy link, Add item to playlist).
- Header: artwork (item image or first image file, generated placeholder fallback, R12), title, creator, year, media-kind badge, language codes, duration, collection link (opens the collection page).
- Action row: **Play all** (primary), **Shuffle**, **Like** (toggle; likes the whole item = all its entries), **Follow** (toggle; follows the item's first collection, or creator when there is none), **Share** (Android share sheet with the archive.org item URL), **Download** (audio items only, R59; state: not downloaded → queued/progress → downloaded; tap on downloaded asks to remove).
- Description: sanitized HTML (R12), collapsed to 4 lines with "More"/"Less".
- Entry list: virtualized; each row has index/track number, title, duration, format/quality hint, liked indicator, download state for audio, and a "⋯" menu. Tapping a row starts the item from that entry (R31). The currently playing entry is highlighted.
- Items with more than 30 entries show a filter field above the list (R32), matching title substrings case-insensitively.

## Entry menu (bottom sheet `sheet_entry`)

Play next · Add to queue · Add to playlist… (opens playlist picker with "New playlist") · Like / Unlike · Download / Remove download (audio only) · Share entry link.

## States

- Loading: header skeleton and placeholder rows; cached metadata renders immediately (R77).
- Nothing playable (R09): `item_nothing_playable` message "This item has nothing ArchivePlayer can play.", Open on archive.org link; Play all, Shuffle, Download disabled; the item is excluded from rows and cannot be added to playlists.
- Error: message with Retry (`item_error`, `item_retry_button`).
- Video items: Download is not shown (R59).
- Adult item while the adult setting is off (e.g. via a shared link, R70): message "This item is hidden by your content settings." with a link to Settings; nothing else loads.

## Collection page

- Opened from an item's collection link, a Home card, a search result, or Following in the Library.
- Header: artwork, title, item count, description (collapsed), **Follow** toggle, Share.
- Item list with the same kind/language/decade/duration filters and sorts as search (UI_SEARCH_SPEC.md), paging 50 per page.

## Text catalog contract

| Key | English |
|---|---|
| `item.playAll` / `item.shuffle` | Play all / Shuffle |
| `item.like` / `item.liked` | Like / Liked |
| `item.follow` / `item.following` | Follow / Following |
| `item.share` | Share |
| `item.download` / `item.downloading` / `item.downloaded` | Download / Downloading {percent} % / Downloaded |
| `item.removeDownload.confirm` | Remove the downloaded files for this item? |
| `item.more` / `item.less` | More / Less |
| `item.filter` | Filter episodes |
| `item.nothingPlayable` | This item has nothing ArchivePlayer can play. |
| `item.openArchive` | Open on archive.org |
| `item.hiddenAdult` | This item is hidden by your content settings. |
| `entry.playNext` / `entry.addToQueue` / `entry.addToPlaylist` | Play next / Add to queue / Add to playlist… |
| `entry.like` / `entry.unlike` | Like / Unlike |
| `entry.download` / `entry.removeDownload` / `entry.share` | Download / Remove download / Share |
| `collection.items` | {count} items |

## Element IDs

| ID | Element |
|---|---|
| `item_screen` / `item_back_button` / `item_overflow_button` | Page root / back / overflow |
| `item_menu_<open_archive\|copy_link\|add_to_playlist>` | Overflow menu actions |
| `item_artwork` / `item_title` / `item_creator` / `item_year` / `item_kind` / `item_languages` / `item_collection_link` | Header fields |
| `item_play_all_button` / `item_shuffle_button` | Playback actions |
| `item_like_button` / `item_follow_button` | Toggles; checked state exposed |
| `item_share_button` / `item_download_button` | Share / download (absent for video) |
| `item_download_progress` | Download progress |
| `item_description` / `item_description_toggle` | Description / More–Less |
| `item_entry_filter` | Episode filter field (only when > 30 entries) |
| `item_entries` | Entry list (scroll container) |
| `item_entry_row_<index>` / `item_entry_menu_<index>` | Entry row / its ⋯ menu; `<index>` = 0-based position in the unfiltered list |
| `item_entry_playing_<index>` | Now-playing highlight |
| `sheet_entry` / `entry_menu_<play_next\|add_to_queue\|add_to_playlist\|like\|download\|share>` | Entry menu and its actions |
| `sheet_playlist_picker` / `playlist_picker_<playlistId>` / `playlist_picker_new` | Playlist picker |
| `item_nothing_playable` / `item_open_archive_button` | Nothing-playable state |
| `item_error` / `item_retry_button` / `item_hidden_adult` | Error / adult-hidden states |
| `collection_screen` / `collection_back_button` / `collection_title` / `collection_count` / `collection_follow_button` / `collection_share_button` | Collection page |
| `collection_items` / `collection_item_<identifier>` | Collection item list / row; filter and sort chips reuse `search_filter_*` / `search_sort_*` inside `collection_screen` |

## Acceptance scenarios

| ID | Scenario and expected result |
|---|---|
| I01 | Open fixture item `OTRR_Philip_Marlowe_Singles` (A11): header shows title, creator, year, audio badge, `en`; entries in `track` order; derivatives grouped into one row each (R10, R11). |
| I02 | Tap `item_play_all_button`: playback starts at entry 0, mini-player shows the entry title, queue contains all entries in order. |
| I03 | Tap `item_entry_row_3`: playback starts at entry 3; the queue holds all entries in item order with entry 3 current, so Previous reaches entry 2. |
| I04 | Shuffle: all entries queued, entry order differs from item order for a fixture with ≥ 5 entries (seeded random in tests). |
| I05 | Like the item, then open Library → Liked: all its entries listed; `item_like_button` checked after reopening. |
| I06 | Follow on an item with a collection: `item_follow_button` checked; the collection appears in Library → Following (A21 part). |
| I07 | Entry menu → Play next / Add to queue: queue order updated accordingly (A13). |
| I08 | Entry menu → Add to playlist → New playlist "Noir": playlist created with that entry; menu → Add to playlist on another entry lists "Noir". |
| I09 | Item with 40 entries: `item_entry_filter` shown; typing "07" shows only matching rows; item with 12 entries has no filter. |
| I10 | Video fixture `his_girl_friday`: `item_download_button` absent; entry menu has no Download. |
| I11 | Audio item Download (Wi-Fi-only satisfied): progress shown, then Downloaded; offline playback of the entry uses the local file (A19 UI part). |
| I12 | Item without playable files (A12): `item_nothing_playable` shown; Play all and Shuffle disabled. |
| I13 | Server error then recovery: `item_error`, Retry loads the page. |
| I14 | Collection page from `item_collection_link`: title, item count, Follow; kind filter Audio limits the list. |
| I15 | Adult fixture reached from an imported share link with the adult setting off (R70): its item page shows `item_hidden_adult`, no entries loaded. |
| I16 | Share opens the Android share sheet with `https://archive.org/details/<identifier>` (intent captured in test). |
