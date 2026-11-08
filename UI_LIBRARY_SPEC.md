# Library, playlists, sharing and downloads

Status: **draft for approval** (gate 2 addition), 8 November 2026. Implements SPECIFICATION.md R44, R50–R61, R85 (UI part), R62. Reference: [interactive mockup](design/archiveplayer-mockup.html) → Library.

## Library tab

- Tab root with the shell top bar: title "Library", theme toggle, Cast (`full`), Settings.
- Filter chips (single choice, remembered): **Playlists** (default), **Liked**, **Following**, **Downloads**, **History**.
- **Playlists:** "New playlist" row first, then Liked (pinned, cannot be renamed or deleted, R52), then user playlists by last change. Row: mosaic artwork (first 4 entries), name, "{count} entries".
- **Liked:** opens the Liked list in the playlist layout.
- **Following:** followed collections and creators; tap opens the collection page (or a creator search); ⋯ → Unfollow.
- **Downloads:** used and free storage for the current location, warning when < 500 MB free (R61); list of downloaded items/entries with size; per-row remove; "Remove all" with confirmation; waiting-for-Wi-Fi rows show "Download now on mobile data" (R60).
- **History:** play events newest first, grouped by day; tap plays the entry; "Clear history" (with confirmation). When history is paused a banner says so with a link to Settings.
- Empty states per chip with a short hint (e.g. "Tap ♡ on anything you like.").

## Playlist page

- Header: mosaic artwork, name, description, "{count} entries · {duration}".
- Actions: Play (primary), Shuffle, Share, Download, Cast (`full`), overflow: Rename, Edit description, Export file, Delete.
- Entry rows as in UI_ITEM_SPEC.md plus a drag handle in edit mode (`playlist_edit_button` toggles); swipe or ⋯ → Remove from playlist.
- Delete: playlist removed, snackbar with Undo (5 s).
- Download of a mixed playlist: only audio entries are queued; a notice states "{n} video entries can't be downloaded" (R59).
- Liked uses this page without Rename/Edit description/Delete.

## Sharing and import (R54–R58)

- Share: if the encoded link fits (≤ 8,000 chars) the Android share sheet opens with the GitHub Pages link; otherwise a dialog explains it is too long and offers "Share as file" (`.archiveplaylist`).
- Export file: Storage Access Framework "Create document" with `<name>.archiveplaylist`.
- Import (opened before setup is complete: the preview appears right after setup): from an App Link (`https://jutlandiatech.github.io/ArchivePlayer/p#…`) or opening/sharing an `.archiveplaylist` file into the app. The **import preview** shows name, description, entry count and the first entries, plus skipped/invalid entries ("{n} entries couldn't be read"); tapping an entry opens its item page; **Import** creates a new playlist (name gets " (2)" when the name exists, never overwrites, R55); **Cancel** leaves everything unchanged.

## Text catalog contract

| Key | English |
|---|---|
| `library.title` | Library |
| `library.filter.playlists` / `.liked` / `.following` / `.downloads` / `.history` | Playlists / Liked / Following / Downloads / History |
| `library.newPlaylist` / `library.newPlaylist.name` | New playlist / Playlist name |
| `library.liked` | Liked |
| `library.entries` | {count} entries |
| `library.empty.playlists` / `.liked` / `.following` / `.downloads` / `.history` | Empty-state hints |
| `library.unfollow` | Unfollow |
| `library.storage` / `library.storage.low` | {used} used · {free} free / Less than 500 MB free |
| `library.downloads.removeAll` / `.removeAll.confirm` | Remove all / Remove all downloads? |
| `library.downloads.waitingWifi` / `.mobileNow` | Waiting for Wi-Fi / Download now on mobile data |
| `library.history.clear` / `.clear.confirm` / `.paused` | Clear history / Clear all history? / History is paused |
| `playlist.play` / `.shuffle` / `.share` / `.download` / `.edit` | Play / Shuffle / Share / Download / Edit |
| `playlist.rename` / `.description` / `.export` / `.delete` / `.deleted.undo` | Rename / Edit description / Export file / Delete / Playlist deleted. Undo |
| `playlist.remove` | Remove from playlist |
| `playlist.videoNotDownloaded` | {count} video entries can't be downloaded |
| `share.tooLong` / `share.asFile` | This playlist is too long for a link. Share it as a file instead. / Share as file |
| `import.title` / `.skipped` / `.import` / `.cancel` | Import playlist / {count} entries couldn't be read / Import / Cancel |

## Element IDs

| ID | Element |
|---|---|
| `library_screen` | Library root (UI_SHELL_SPEC.md) |
| `library_filter_<playlists\|liked\|following\|downloads\|history>` | Chips; selected state exposed |
| `library_list` | List (scroll container) |
| `library_new_playlist_button` / `dialog_playlist_name_field` / `dialog_confirm_button` / `dialog_cancel_button` | New playlist + shared text/confirm dialogs |
| `library_playlist_<playlistId>` | Playlist row; Liked = `library_playlist_liked` |
| `library_following_<sourceId>` / `library_unfollow_<sourceId>` | Followed source row / unfollow |
| `library_storage_summary` / `library_storage_warning` | Storage usage / low-space warning |
| `library_download_<identifier>` / `library_download_remove_<identifier>` / `library_download_mobile_now_<identifier>` | Download rows |
| `library_downloads_remove_all` | Remove all |
| `library_history_<index>` / `library_history_clear` / `library_history_paused` | History rows / clear / paused banner |
| `library_empty` | Empty state of the current chip |
| `playlist_screen` / `playlist_back_button` / `playlist_title` / `playlist_description` / `playlist_count` | Playlist page header |
| `playlist_play_button` / `playlist_shuffle_button` / `playlist_share_button` / `playlist_download_button` / `playlist_cast_button` | Actions (`playlist_cast_button` only in `full`) |
| `playlist_overflow_button` / `playlist_menu_<rename\|description\|export\|delete>` | Overflow and its actions |
| `playlist_edit_button` / `playlist_entry_row_<index>` / `playlist_entry_drag_<index>` / `playlist_entry_remove_<index>` | Entry editing |
| `playlist_video_notice` | Mixed-download notice |
| `playlist_undo_delete` | Undo snackbar |
| `dialog_share_too_long` / `share_as_file_button` | Too-long link dialog |
| `import_screen` / `import_title` / `import_count` / `import_skipped` / `import_entry_<index>` / `import_confirm_button` / `import_cancel_button` | Import preview |

## Acceptance scenarios

| ID | Scenario and expected result |
|---|---|
| L01 | Open Library: chips in order Playlists, Liked, Following, Downloads, History; Playlists selected; `library_playlist_liked` first after New playlist. |
| L02 | New playlist "Noir": `library_playlist_<id>` row appears with "0 entries"; the playlist page opens. |
| L03 | Add 3 entries to a playlist from item pages; reorder entry 2 to position 0 in edit mode; remove entry 1: order persists after app restart. |
| L04 | Rename and edit description through the overflow: header and Library row update. |
| L05 | Delete a playlist: gone from Library; Undo restores it with entries. Liked has no Delete/Rename. |
| L06 | Play / Shuffle on a playlist: queue replaced with the playlist entries (R35). |
| L07 | Like an entry: Liked chip lists it; unliking removes it. |
| L08 | Following lists followed collections; Unfollow removes the row and the source stops feeding Home (R53). |
| L09 | Share a 20-entry playlist (A21): share-sheet intent carries a `https://jutlandiatech.github.io/ArchivePlayer/p#…` link that decodes to the same 20 entries. |
| L10 | Share a playlist whose link exceeds 8,000 characters (A23): `dialog_share_too_long` with Share as file; the file intent has an `.archiveplaylist` attachment. |
| L11 | Open a share link via `ACTION_VIEW` intent: `import_screen` with the playlist name and count; Import creates a new playlist; importing again creates "Name (2)" (A21, R55). |
| L12 | Open an `.archiveplaylist` file with 2 invalid entries (A24): preview reports 2 skipped; import creates the valid entries only. |
| L13 | Download a mixed playlist on mobile data with Wi-Fi-only on (A25): only audio entries listed in Downloads, rows show Waiting for Wi-Fi and the mobile-data action; `playlist_video_notice` shown. |
| L14 | Downloads chip: storage summary visible; with < 500 MB free (fake storage) `library_storage_warning` shown; remove one and Remove all work. |
| L15 | History chip lists played entries newest first; Clear history empties it; with history paused the banner is shown. |
| L16 | Empty Library chips show `library_empty` with the catalog hint in all three languages. |
| L17 | `foss`: `playlist_cast_button` absent; `full`: present. |
