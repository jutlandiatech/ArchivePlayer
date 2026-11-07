# Navigation shell, theme and element-ID contract

Status: **draft v0.2 for final review**, 7 November 2026. Companion to SPECIFICATION.md R18–R26, R34–R38, R46, R50, R62–R69, R80–R83 and decisions D17, D22, D23, D26. Not yet approved.

## Top-level structure

- Three destinations, in this order: **Home**, **Search** (search plus category browse), **Library** (playlists, liked, following, downloads, history).
- Phones (< 600 dp width): Material 3 bottom navigation bar. Tablets/foldables (≥ 600 dp): navigation rail on the start edge.
- Each destination keeps its own back stack; reselecting the current tab scrolls to top and then pops to the tab root. Android Back pops within the tab, then returns to Home, then leaves the app.
- Top bar on each tab root: localized title, theme toggle (sun/moon, R82), Cast button (Media3 `MediaRouteButton`, `full` flavour only), and on Library a Settings action.
- Detail destinations (item, collection, category, playlist, settings pages) open within the current tab's back stack.

## Mini-player and full player

- While a queue exists, a mini-player bar sits directly above the bottom bar (phones) or at the bottom of the content area (tablets) on every screen except the full player and setup.
- Mini-player content: artwork thumbnail, title, creator/item, play/pause, a thin progress indicator, and a Cast indicator when casting. Tapping it, or swiping it up, opens the full player.
- Full player (phones): full-screen sheet with artwork or video surface, title, creator, like, seek bar, controls (R37), queue, quality, subtitles, speed, sleep timer, Cast. Swiping down collapses it to the mini-player.
- Full player (tablets, landscape): side pane beside the content, so browsing continues while the player stays visible.
- Video entries: tapping the video surface toggles controls; a full-screen button switches to immersive landscape.

## Theme

Design language follows jutlandiatech/sim-warning-app (SPECIFICATION.md R63, R83).

- **Nordic light** (default, regardless of the system setting): white-dominant surfaces, black text, medium blue `#2769AD` for primary actions, selection, icons and progress.
- **Nordic noir** (dark mode): dark neutral surfaces, light text, the same blue family as accent, lightened where needed for WCAG AA contrast.
- **Switching:** a sun/moon theme toggle in the top bar of every tab root flips light ↔ noir in one tap. Settings → Appearance offers Nordic light, Nordic noir and Follow system. The change applies immediately without losing navigation, scroll or playback state, and persists.
- No dynamic (wallpaper) colour.
- Typography: body text 18 sp, headings about 28 sp, Android font scaling respected; controls at least 48 dp; generous spacing as in sim-warning-app. Artwork has rounded corners (about 8 dp) for items and is circular for creators/collections.
- All colours are defined in one theme file; no hard-coded colours in screens.

## Flavour differences

- `full`: Cast buttons in the top bar and the player, Cast indicator in the mini-player.
- `foss`: no Cast button, no Cast indicator, no route discovery; the layout closes the gap (no empty slot). All other shell behavior is identical.

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
| `top_bar_theme_toggle` | Light/noir toggle; current theme exposed as state |
| `top_bar_cast_button` | Cast route button (`full` only) |
| `top_bar_settings_button` | Settings action (top bar of every tab root: Home, Search, Library) |
| `home_screen` / `search_screen` / `library_screen` | Tab root screens |
| `mini_player` | Mini-player bar |
| `mini_player_title` | Current entry title |
| `mini_player_play_pause_button` | Play/pause; state exposed |
| `mini_player_progress` | Progress indicator |
| `mini_player_cast_indicator` | Visible while casting (`full` only) |
| `player_screen` | Full player root |
| `player_collapse_button` | Collapse to mini-player |
| `player_video_surface` / `player_artwork` | Video surface or audio artwork |
| `player_play_pause_button` / `player_previous_button` / `player_next_button` | Transport controls |
| `player_skip_back_button` / `player_skip_forward_button` | Skip controls |
| `player_seek_bar` | Seek bar with progress semantics |
| `player_like_button` | Like toggle |
| `player_queue_button` / `player_quality_button` / `player_subtitles_button` / `player_speed_button` / `player_sleep_timer_button` | Player menus |
| `player_shuffle_button` / `player_repeat_button` | Shuffle / repeat; state exposed |
| `player_cast_button` | Cast route button in the player (`full` only) |
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
| N10 | Fresh install with the phone in dark mode: app opens in Nordic light. Tap `top_bar_theme_toggle`: Nordic noir applies immediately on every screen without losing state; persists after restart. With Appearance = Follow system, a system dark/light switch is followed live. |
| N11 | All three UI languages, 200% font scale, 320 dp width: no clipping in bars and mini-player; touch targets ≥ 48 dp; IDs unchanged. |
| N12 | TalkBack: destinations, theme toggle, mini-player and player controls have localized labels and correct selected/toggle states. |
| N13 | `foss` build: `top_bar_cast_button`, `player_cast_button` and `mini_player_cast_indicator` do not exist; no empty gaps; all other N-scenarios pass. |

## Resolved points

| ID | Topic | Resolution |
|---|---|---|
| UO1 | Tablet player placement | Side pane on the end edge at ≥ 840 dp; full-screen sheet below that. |
| UO2 | Mini-player gestures | Swipe up opens the player; horizontal swipe skips to previous/next. |
