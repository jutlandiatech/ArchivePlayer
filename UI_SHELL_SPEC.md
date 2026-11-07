# Navigation shell, theme and element-ID contract

Status: **draft v0.1 for review**, 7 November 2026. Companion to SPECIFICATION.md R18–R26, R34–R38, R46, R50, R62–R69 and decision D17. Not approved; open points are listed at the end.

## Top-level structure

- Three destinations, in this order: **Home**, **Search** (search plus category browse), **Library** (playlists, liked, following, downloads, history).
- Phones (< 600 dp width): Material 3 bottom navigation bar. Tablets/foldables (≥ 600 dp): navigation rail on the start edge.
- Each destination keeps its own back stack; reselecting the current tab scrolls to top and then pops to the tab root. Android Back pops within the tab, then returns to Home, then leaves the app.
- Top bar on each tab root: localized title, Cast button (Media3 `MediaRouteButton`), and on Library a Settings action.
- Detail destinations (item, collection, category, playlist, settings pages) open within the current tab's back stack.

## Mini-player and full player

- While a queue exists, a mini-player bar sits directly above the bottom bar (phones) or at the bottom of the content area (tablets) on every screen except the full player and setup.
- Mini-player content: artwork thumbnail, title, creator/item, play/pause, a thin progress indicator, and a Cast indicator when casting. Tapping it, or swiping it up, opens the full player.
- Full player (phones): full-screen sheet with artwork or video surface, title, creator, like, seek bar, controls (R37), queue, quality, subtitles, speed, sleep timer, Cast. Swiping down collapses it to the mini-player.
- Full player (tablets, landscape): side pane beside the content, so browsing continues while the player stays visible.
- Video entries: tapping the video surface toggles controls; a full-screen button switches to immersive landscape.

## Theme

- Dark theme by default, light theme when the system is light (follow system). No dynamic colour.
- One brand accent colour (O02 in SPECIFICATION.md) for primary actions, selection and progress. Surfaces: Material 3 tonal surfaces derived from neutral greys.
- Typography: Material 3 type scale; body text at least 16 sp; Android font scaling respected. Artwork has rounded corners (about 8 dp) for items and is circular for creators/collections.
- All colours are defined in one theme file; no hard-coded colours in screens.

## Element-ID contract

Follows the sim-warning-app approach (SPECIFICATION.md R69):

- IDs are lower snake case: `<area>_<element>[_<qualifier>]`, for example `nav_home`, `player_play_pause_button`, `search_filter_language_fr`.
- IDs live in one Kotlin registry object (`ElementIds`), attached with `Modifier.testTag(...)`. `testTagsAsResourceId = true` is set on each semantic root, including dialogs and popups, so UI Automator can use `By.res(...)`.
- Rows in lists use a qualifier from stable data (archive.org identifier or local playlist ID), never the position or localized text: `item_entry_row_<index-in-item>` is allowed only for entries because entry order is defined by R11.
- IDs are independent of language, theme and selected values. Accessibility labels stay localized and meaningful; IDs are never read aloud.

### Shell registry (initial)

| ID | Element |
|---|---|
| `app_root` | Root of the main UI after setup |
| `nav_bar` / `nav_rail` | Bottom navigation bar (phone) / navigation rail (tablet) |
| `nav_home` / `nav_search` / `nav_library` | Destination items; selected state exposed |
| `top_bar_title` | Localized title of the current tab root |
| `top_bar_cast_button` | Cast route button |
| `top_bar_settings_button` | Settings action (Library) |
| `home_screen` / `search_screen` / `library_screen` | Tab root screens |
| `mini_player` | Mini-player bar |
| `mini_player_title` | Current entry title |
| `mini_player_play_pause_button` | Play/pause; state exposed |
| `mini_player_progress` | Progress indicator |
| `mini_player_cast_indicator` | Visible while casting |
| `player_screen` | Full player root |
| `player_collapse_button` | Collapse to mini-player |
| `player_video_surface` / `player_artwork` | Video surface or audio artwork |
| `player_play_pause_button` / `player_previous_button` / `player_next_button` | Transport controls |
| `player_skip_back_button` / `player_skip_forward_button` | Skip controls |
| `player_seek_bar` | Seek bar with progress semantics |
| `player_like_button` | Like toggle |
| `player_queue_button` / `player_quality_button` / `player_subtitles_button` / `player_speed_button` / `player_sleep_timer_button` | Player menus |
| `player_shuffle_button` / `player_repeat_button` | Shuffle / repeat; state exposed |
| `player_cast_button` | Cast route button in the player |
| `player_error` / `player_retry_button` | Playback error and retry (R43) |

Area registries for Home, Search, Item, Library, Playlist and Settings follow in their own UI specs.

## Acceptance scenarios

| ID | Scenario and expected result |
|---|---|
| N01 | Phone portrait after setup: bottom bar with three destinations in order; Home selected; IDs discoverable via Compose and UI Automator. |
| N02 | Tablet (≥ 600 dp) and phone landscape at ≥ 600 dp: navigation rail replaces the bottom bar; same IDs for the destinations. |
| N03 | Navigate Home → item → collection, switch to Search and back to Home: Home's back stack is preserved. |
| N04 | Reselect the active tab: scroll to top, then pop to the root. |
| N05 | Android Back from a tab root other than Home returns to Home; from Home it leaves the app. |
| N06 | Start playback: mini-player appears on every tab and detail screen; not on setup or the full player. |
| N07 | Tap or swipe up the mini-player: full player opens; swipe down or collapse returns; playback is uninterrupted. |
| N08 | Mixed queue advances from audio to video: player switches from artwork to video surface. |
| N09 | Cast session active: Cast indicator in mini-player; controls act on the receiver. |
| N10 | System dark/light switch while the app is open: theme follows without losing state. |
| N11 | All three UI languages, 200% font scale, 320 dp width: no clipping in bars and mini-player; touch targets ≥ 48 dp; IDs unchanged. |
| N12 | TalkBack: destinations, mini-player and player controls have localized labels and correct selected/toggle states. |

## Open points

| ID | Topic | Proposed default |
|---|---|---|
| UO1 | Tablet player placement | Side pane on the end edge at ≥ 840 dp; full-screen sheet below that. |
| UO2 | Mini-player swipe gestures | Swipe up opens the player; horizontal swipe skips to previous/next (Spotify-style). |
