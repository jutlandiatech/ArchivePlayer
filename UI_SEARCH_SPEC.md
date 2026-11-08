# Search and browse

Status: **approved** (gate 2), 8 November 2026. Implements SPECIFICATION.md R09, R14, R22–R29, R33, R62, R70–R71, R77. Reference: [interactive mockup](design/archiveplayer-mockup.html) → Search.

## Layout

- Tab root with the shell top bar: title "Search", theme toggle, Cast (`full`), Settings.
- Query field (56 dp, rounded, search icon, clear button when not empty), placeholder "What do you want to play?".
- **Without a query:** recent searches (up to 5, each removable, R29) followed by "Browse all": a grid of category tiles from the category map's top-level and second-level categories (two columns on phones, adaptive on tablets), each tile with the localized name and media kind (R26).
- **While typing:** local suggestions below the field (search history, followed collections, category names), debounced 300 ms, no network request (R22). Choosing a suggestion or pressing the keyboard search action runs the search and stores the query in recent searches.
- **Results:** a filter chip row and a sort chip row, then grouped sections Collections, Items, Your playlists (R25). Items page incrementally while scrolling (50 per page). Each item row: artwork 56 dp, title, kind badge, creator · year · language codes.
- Tapping an item opens the item page; a collection opens the collection page (UI_ITEM_SPEC.md); a playlist opens the playlist page (UI_LIBRARY_SPEC.md); all in the Search back stack.

## Filters and sort (R23, R24, R28)

- Media kind: All (default), Audio, Video — single choice.
- Language: "Any language" (default) plus chips for the preferred content languages and "More…" for the full list; single choice in v1. With a language filter active, untagged items are excluded (R28).
- Category, decade and duration open small menus from chips "Category", "Decade", "Duration"; the chosen value replaces the chip label and the chip shows a remove (×) action.
- Filters combine with AND; every active filter stays visible and individually removable; "Clear filters" resets all.
- Sort: Relevance (default), Popularity, Newest added, Original date — single choice.
- Filters and sort survive navigation to an item and back, and rotation.

## Category page

- Opened from a browse tile or a category link: back action, category name, sub-category chips (if any), then results for the category query with the same filter and sort rows (except the Category chip).
- Tapping a sub-category chip opens that sub-category's page.

## States

- Empty result: "No results for "{query}"" plus "Clear filters" when filters are active (`search_empty`).
- Error / rate limited (HTTP 429/5xx after back-off, R76): inline message with Retry; the query and filters stay.
- Loading longer than 15 s: "Still loading" with Retry (R77).
- Paging error: the loaded results stay; a footer shows Retry.
- Adult content hidden (default): adult items never appear (R70, R71); no indication that something was filtered.

## Text catalog contract

| Key | English |
|---|---|
| `search.title` | Search |
| `search.placeholder` | What do you want to play? |
| `search.recent` | Recent searches |
| `search.browse` | Browse all |
| `search.filter.kind.all` / `.audio` / `.video` | All / Audio / Video |
| `search.filter.language.any` | Any language |
| `search.filter.category` / `.decade` / `.duration` | Category / Decade / Duration |
| `search.filter.duration.short` / `.episode` / `.feature` | Under 20 min / 20–60 min / Over 60 min |
| `search.filter.clear` | Clear filters |
| `search.sort.relevance` / `.popularity` / `.newest` / `.date` | Relevance / Popularity / Newest added / Original date |
| `search.section.collections` / `.items` / `.playlists` | Collections / Items / Your playlists |
| `search.empty` | No results for "{query}" |
| `search.error` | Search failed. Check your connection and try again. |
| `search.stillLoading` | Still loading… |

## Element IDs

| ID | Element |
|---|---|
| `search_screen` | Search root (UI_SHELL_SPEC.md) |
| `search_query_field` / `search_clear_button` | Query field / clear |
| `search_recent_<index>` / `search_recent_remove_<index>` | Recent search row / remove |
| `search_suggestion_<index>` | Suggestion row |
| `browse_grid` / `browse_tile_<categoryId>` | Browse grid / tile |
| `search_filter_kind_<all\|audio\|video>` | Kind chips; selected state exposed |
| `search_filter_language_<any\|iso>` / `search_filter_language_more` | Language chips |
| `search_filter_category` / `search_filter_decade` / `search_filter_duration` | Menu chips; `_remove` suffix for their × action |
| `search_filter_option_<value>` | Option in an open filter menu or the language "More…" list (category ID, language ISO code, decade `1950`, duration `short\|episode\|feature`) |
| `search_filter_clear` | Clear filters |
| `search_sort_<relevance\|popularity\|newest\|date>` | Sort chips; selected state exposed |
| `search_results` | Results list (scroll container) |
| `search_section_<collections\|items\|playlists>` | Section headings |
| `search_result_<identifier>` | Item or collection result row |
| `search_result_playlist_<playlistId>` | Own-playlist result row |
| `search_empty` / `search_error` / `search_retry_button` / `search_paging_retry_button` | States |
| `category_screen` / `category_back_button` / `category_title` | Category page |
| `category_subcategory_<categoryId>` | Sub-category chip |

The mockup's `search_sort_rel/pop/new` are renamed to the full words above.

## Acceptance scenarios

| ID | Scenario and expected result |
|---|---|
| Q01 | Open Search without a query: browse grid shows one tile per category-map entry (bundled fixture map), localized names; recent searches hidden when empty. |
| Q02 | Type "gun" with history containing "gunsmoke": suggestion appears; no request reaches the fixture server while typing (R22). |
| Q03 | Search "gunsmoke" (A10), filter Audio, sort Popularity: request contains `mediatype:(audio OR etree)` and `sort[]=downloads desc`; results in server order; sections Collections / Items / Your playlists in that order, empty sections hidden. |
| Q04 | Language filter French on a fixture with fr, `French`, untagged items (A05–A07): only French-tagged items shown; switching to Any language shows the untagged item too. |
| Q05 | Several filters active: each chip shows its value and × removes only that filter; Clear filters resets all. |
| Q06 | Scroll to the end of page 1: page 2 is requested and appended; no duplicate rows. |
| Q07 | Server returns 503 then 200: after back-off the results show; if it keeps failing, `search_error` with Retry; Retry succeeds after recovery. |
| Q08 | Query with no hits: `search_empty` with the query text from the catalog; with filters active `search_filter_clear` is offered. |
| Q09 | Fixture contains an item in `no-preview` and one with subject "xxx" (A27): neither is shown with the adult setting off. |
| Q10 | Recent searches: running a search adds it; `search_recent_remove_0` removes it; Settings → Search history shows the same list (R29). |
| Q11 | Tap a browse tile with sub-categories: category page with sub-category chips and results; Back returns to the grid. |
| Q12 | Open an item from results and go back: query, filters, sort and scroll position preserved; also after rotation. |
| Q13 | Own playlist named "Gunsmoke favourites": appears under Your playlists for "gunsmoke". |
| Q14 | All three languages at 200 % font scale, 320 dp width: chips wrap or scroll without clipping; IDs unchanged. |
