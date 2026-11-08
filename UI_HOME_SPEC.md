# Home screen

Status: **approved** (gate 2), 8 November 2026. Implements SPECIFICATION.md R18–R21, R36, R44, R53, R62, R77. Reference: [interactive mockup](design/archiveplayer-mockup.html) → Home.

## Layout

- Tab root with the shell top bar (UI_SHELL_SPEC.md): title "Home", theme toggle, Cast (`full`), Settings.
- A vertical, lazily composed list of rows. Each row has a heading (22 sp) and a horizontally scrolling list of cards. Rows and cards are lazy, so long rows cost nothing off-screen.
- Card: square artwork 136 dp with rounded corners (circular for collections/creators), media-kind badge (♪ audio, ▶ video), title (max 2 lines), secondary line (creator, year, or progress text), and for Continue cards a progress bar.
- Pull-to-refresh refreshes curated rows and "New in collections you follow" (R21: at most every 6 hours, manual refresh always allowed).
- Tapping a card opens the item page (UI_ITEM_SPEC.md) in the Home back stack; a playlist card opens the playlist page (UI_LIBRARY_SPEC.md); a collection card opens the collection page.
- Long-press on an item card opens the entry/item menu (Play, Play next, Add to queue, Add to playlist, Like) from UI_ITEM_SPEC.md.

## Row order and visibility

Order (R18), personal rows hidden when empty:

1. Continue listening & watching — entries with a stored resume position that are not finished (R36), newest first; secondary text "{position} / {duration}"; tapping resumes at the stored position.
2. Recently played — newest first, de-duplicated by item.
3. Your playlists — Liked first, then user playlists by last change; mosaic artwork.
4. Downloads — downloaded audio items (shown only when at least one exists).
5. New in collections you follow — items added since the last check (R21).
6. Because you played {title} — on-device recommendations (R19), at most one row in v1.
7. Curated rows: one row per top-level category of the category map, in map order (R14–R17), ranked by preferred content languages without hiding untagged items (R20).

## States

- First start (no history): only curated rows; no empty placeholders for personal rows.
- Loading: cached rows render immediately (R77); rows still loading show placeholder cards (`home_row_loading_<row>`); after 15 s a row shows "Still loading" with Retry.
- Offline / error: cached rows stay visible; rows without cache show an inline error with Retry (`home_row_error_<row>`, `home_row_retry_<row>`). No full-screen error while any row has content.
- History paused (Settings): Continue and Recently played still show what was recorded before the pause.

## Text catalog contract

| Key | English |
|---|---|
| `home.title` | Home |
| `home.row.continue` | Continue listening & watching |
| `home.row.recent` | Recently played |
| `home.row.playlists` | Your playlists |
| `home.row.downloads` | Downloads |
| `home.row.newFollowed` | New in collections you follow |
| `home.row.because` | Because you played {title} |
| `home.card.progress` | {position} / {duration} |
| `home.row.stillLoading` | Still loading… |
| `home.row.error` | Couldn't load this row. |
| `action.retry` | Retry |

Curated row headings come from the category map's localized names (R68).

## Element IDs

| ID | Element |
|---|---|
| `home_screen` | Home root (UI_SHELL_SPEC.md) |
| `home_list` | Vertical row list (scroll container) |
| `home_row_<row>` | Row container; `<row>` = `continue`, `recent`, `playlists`, `downloads`, `new_followed`, `because`, or `category_<categoryId>` |
| `home_row_title_<row>` | Row heading |
| `home_card_<row>_<identifier>` | Item card in a row (archive.org identifier) |
| `home_playlist_<playlistId>` | Playlist card (`liked` for the Liked list) |
| `home_card_progress_<identifier>` | Progress bar on a Continue card |
| `home_row_loading_<row>` / `home_row_error_<row>` / `home_row_retry_<row>` | Loading, error and retry per row |
| `home_refresh` | Pull-to-refresh indicator |

The mockup's `home_card_<identifier>` is refined to include the row so that an item appearing in two rows keeps unique IDs.

## Acceptance scenarios

| ID | Scenario and expected result |
|---|---|
| H01 | Fresh install, fixture category map and search responses: Home shows only curated rows, in category-map order; no personal row IDs exist. |
| H02 | Play an entry for more than 30 s, stop, return to Home: `home_row_continue` is first and contains the item with a progress bar; `home_row_recent` follows. |
| H03 | Tap a Continue card: player opens on that entry at the stored position (± 2 s). |
| H04 | Finish an entry (last 30 s or 95 %): it leaves Continue and stays in Recently played. |
| H05 | Create a playlist and like an entry: `home_row_playlists` shows `home_playlist_liked` first, then the playlist. |
| H06 | Follow a collection: `home_row_new_followed` is fed from it; reopening Home within 6 hours sends no new search for that collection; pull-to-refresh sends one (R21). |
| H07 | Content languages = French; a curated row's fixture has French, English and untagged items: French items come first; untagged items are still shown. |
| H08 | Server returns errors for one curated row: the other rows render; that row shows error and Retry; Retry after the server recovers fills the row. |
| H09 | Slow fixture response (> 15 s) for a row: "Still loading" with Retry appears; the rest of Home stays usable. |
| H10 | Tap a curated item card: item page opens in the Home tab; Back returns to Home at the same scroll position. |
| H11 | Pause history in Settings, play another entry: Continue and Recently played do not change. |
| H12 | All three languages at 200 % font scale, 320 dp width: row headings and cards do not clip; IDs unchanged. |
