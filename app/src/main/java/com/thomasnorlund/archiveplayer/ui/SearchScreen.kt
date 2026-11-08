package com.thomasnorlund.archiveplayer.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.thomasnorlund.archiveplayer.AppViewModel
import com.thomasnorlund.archiveplayer.Screen
import com.thomasnorlund.archiveplayer.core.catalog.DurationClass
import com.thomasnorlund.archiveplayer.core.catalog.MediaKind
import com.thomasnorlund.archiveplayer.core.catalog.SearchHit
import com.thomasnorlund.archiveplayer.core.language.LanguageNormalizer
import com.thomasnorlund.archiveplayer.core.search.KindFilter
import com.thomasnorlund.archiveplayer.core.search.SearchRequest
import com.thomasnorlund.archiveplayer.core.search.SortOrder

/** Selectable chip exposing Selected semantics (tests assert isSelected). */
/** Tag plus own text on one semantics node, so tests read the label from the tagged node (unmerged tree). */
fun Modifier.tagged(tag: String, label: String): Modifier = clearAndSetSemantics {
    testTag = tag
    text = AnnotatedString(label)
}

@Composable
fun SelectChip(tag: String, label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.heightIn(min = 48.dp).selectable(selected = selected, role = Role.Tab, onClick = onClick).tagged(tag, label),
    ) {
        Box(Modifier.padding(horizontal = 14.dp, vertical = 12.dp), contentAlignment = Alignment.Center) {
            Text(label, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
fun GlyphButton(tag: String, glyph: String, description: String, onClick: () -> Unit) {
    IconButton(onClick = onClick, modifier = Modifier.testTag(tag).semantics { contentDescription = description }) {
        Text(glyph, style = MaterialTheme.typography.titleLarge)
    }
}

@Composable
fun HitRow(hit: SearchHit, tag: String, onClick: () -> Unit) {
    val badge = when (hit.kind) { MediaKind.AUDIO -> "♪ "; MediaKind.VIDEO -> "▶ "; null -> "▦ " }
    val secondary = listOfNotNull(hit.creator, hit.date?.take(4)).joinToString(" · ")
    Column(
        Modifier.fillMaxWidth().heightIn(min = 56.dp).clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 4.dp)
            .tagged(tag, listOf(hit.title, secondary).filter { it.isNotEmpty() }.joinToString(", ")),
    ) {
        Text(badge + hit.title, style = MaterialTheme.typography.bodyLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
        if (secondary.isNotEmpty()) Text(secondary, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

fun openHit(vm: AppViewModel, hit: SearchHit) =
    vm.push(if (hit.kind == null) Screen.Collection(hit.identifier) else Screen.Item(hit.identifier))

@Composable
fun SearchTab(vm: AppViewModel) {
    val s = vm.search
    Column(Modifier.fillMaxSize().testTag(ElementIds.SEARCH_SCREEN)) {
        OutlinedTextField(
            value = s.query,
            onValueChange = { s.query = it },
            singleLine = true,
            placeholder = { Text(t("search.placeholder")) },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { s.submit() }),
            trailingIcon = if (s.query.isNotEmpty() || s.submitted != null) {
                { GlyphButton(ElementIds.SEARCH_CLEAR_BUTTON, "✕", t("action.cancel")) { s.clear() } }
            } else null,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp).testTag(ElementIds.SEARCH_QUERY_FIELD),
        )
        val suggestions = s.suggestions()
        when {
            suggestions.isNotEmpty() -> LazyColumn(Modifier.fillMaxSize()) {
                itemsIndexed(suggestions) { i, text ->
                    Text(
                        text,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable { s.submit(text) }
                            .padding(horizontal = 16.dp, vertical = 12.dp).testTag(ElementIds.searchSuggestion(i)),
                    )
                }
            }
            s.submitted == null -> BrowseGrid(vm)
            else -> SearchResults(vm)
        }
    }
}

@Composable
private fun BrowseGrid(vm: AppViewModel) {
    val s = vm.search
    val lang = vm.settings?.appLanguage ?: "en"
    val recent = vm.library.state.recentSearches
    val tiles = vm.repo.categoryMap.roots.flatMap { listOf(it) + it.children }
    LazyVerticalGrid(GridCells.Adaptive(150.dp), Modifier.fillMaxSize().testTag(ElementIds.BROWSE_GRID)) {
        if (recent.isNotEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) { SectionTitle(t("search.recent")) }
            itemsIndexed(recent, span = { _, _ -> GridItemSpan(maxLineSpan) }) { i, q ->
                Row(Modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        q,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f).clickable { s.submit(q) }.padding(horizontal = 16.dp, vertical = 12.dp)
                            .testTag(ElementIds.searchRecent(i)),
                    )
                    GlyphButton(ElementIds.searchRecentRemove(i), "✕", t("action.delete")) { s.removeRecent(i) }
                }
            }
        }
        item(span = { GridItemSpan(maxLineSpan) }) { SectionTitle(t("search.browse")) }
        items(tiles, key = { it.id }) { c ->
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (c.children.isNotEmpty()) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.padding(6.dp).heightIn(min = 72.dp).fillMaxWidth()
                    .clickable { vm.push(Screen.Category(c.id)) }.tagged(ElementIds.browseTile(c.id), c.name(lang)),
            ) {
                Box(Modifier.padding(12.dp), contentAlignment = Alignment.CenterStart) {
                    Text(c.name(lang), style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}

@Composable
fun SectionTitle(text: String, tag: String? = null) {
    Text(
        text,
        style = MaterialTheme.typography.titleLarge,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 4.dp).let { if (tag != null) it.testTag(tag) else it },
    )
}

private val DECADES = (1900..2020 step 10).toList()

@Composable
private fun SearchFilters(vm: AppViewModel) {
    val s = vm.search
    var picker by rememberSaveable { mutableStateOf<String?>(null) }
    val contentLanguages = vm.settings?.contentLanguages.orEmpty().sorted()
    Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
        Row(
            Modifier.horizontalScroll(rememberScrollState()).padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            KindFilter.entries.forEach { k ->
                val key = k.name.lowercase()
                SelectChip(ElementIds.searchFilterKind(key), t("search.filter.kind.$key"), s.kind == k) { s.chooseKind(k) }
            }
            SelectChip(ElementIds.searchFilterLanguage("any"), t("search.filter.language.any"), s.languages.isEmpty()) { s.chooseLanguage(null) }
            (contentLanguages + s.languages).distinct().forEach { iso ->
                SelectChip(ElementIds.searchFilterLanguage(iso), iso, iso in s.languages) { s.chooseLanguage(iso) }
            }
            SelectChip(ElementIds.SEARCH_FILTER_LANGUAGE_MORE, t("search.filter.language") + " …", picker == "language") {
                picker = if (picker == "language") null else "language"
            }
            FilterValueChip(ElementIds.SEARCH_FILTER_DECADE, t("search.filter.decade"), s.decade?.let { "${it}s" }, picker == "decade",
                onClick = { picker = if (picker == "decade") null else "decade" }, onRemove = { s.chooseDecade(null) })
            FilterValueChip(ElementIds.SEARCH_FILTER_DURATION, t("search.filter.duration"),
                s.duration?.let { t("search.filter.duration.${it.name.lowercase()}") }, picker == "duration",
                onClick = { picker = if (picker == "duration") null else "duration" }, onRemove = { s.chooseDuration(null) })
            if (s.hasFilters) {
                TextButton(onClick = { picker = null; s.clearFilters() }, modifier = Modifier.heightIn(min = 48.dp).testTag(ElementIds.SEARCH_FILTER_CLEAR)) {
                    Text(t("search.filter.clear"))
                }
            }
            listOf(SortOrder.RELEVANCE, SortOrder.POPULARITY, SortOrder.NEWEST).forEach { o ->
                val key = o.name.lowercase()
                SelectChip(ElementIds.searchSort(key), t("search.sort.$key"), s.sort == o) { s.chooseSort(o) }
            }
        }
        val options: List<Pair<String, String>> = when (picker) {
            "language" -> LanguageNormalizer.knownCodes.sorted().map { it to (LanguageNormalizer.englishName(it) ?: it) }
            "decade" -> DECADES.map { "$it" to "${it}s" }
            "duration" -> DurationClass.entries.map { it.name.lowercase() to t("search.filter.duration.${it.name.lowercase()}") }
            else -> emptyList()
        }
        if (options.isNotEmpty()) {
            Row(Modifier.horizontalScroll(rememberScrollState()).padding(bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                options.forEach { (value, label) ->
                    SelectChip(ElementIds.searchFilterOption(value), label, false) {
                        when (picker) {
                            "language" -> s.chooseLanguage(value)
                            "decade" -> s.chooseDecade(value.toInt())
                            "duration" -> s.chooseDuration(DurationClass.valueOf(value.uppercase()))
                        }
                        picker = null
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterValueChip(tag: String, label: String, value: String?, open: Boolean, onClick: () -> Unit, onRemove: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        SelectChip(tag, if (value == null) "$label ▾" else "$label: $value", value != null || open, onClick)
        if (value != null) GlyphButton(ElementIds.searchFilterRemove(tag), "✕", t("action.delete"), onRemove)
    }
}

@Composable
private fun SearchResults(vm: AppViewModel) {
    val s = vm.search
    Column(Modifier.fillMaxSize()) {
        SearchFilters(vm)
        val grouped = s.grouped()
        when {
            s.error -> ErrorState(ElementIds.SEARCH_ERROR, t("search.error"), ElementIds.SEARCH_RETRY_BUTTON) { s.retry() }
            s.loading && s.hits.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            s.hits.isEmpty() && grouped.playlists.isEmpty() -> Text(
                t("search.empty", "query" to s.submitted.orEmpty()),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(24.dp).testTag(ElementIds.SEARCH_EMPTY),
            )
            else -> {
                val state = rememberLazyListState()
                LaunchedEffect(state, s.hits.size) {
                    snapshotFlow { state.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0 }
                        .collect { last -> if (last >= state.layoutInfo.totalItemsCount - SearchController.ROWS) s.loadMore() }
                }
                val collectionsTitle = t("search.section.collections")
                val itemsTitle = t("search.section.items")
                LazyColumn(Modifier.fillMaxSize().testTag(ElementIds.SEARCH_RESULTS), state = state) {
                    section(this, "collections", collectionsTitle, grouped.collections) { openHit(vm, it) }
                    if (grouped.playlists.isNotEmpty()) {
                        item(key = "h_playlists") { SectionTitle(t("search.section.playlists"), ElementIds.searchSection("playlists")) }
                        items(grouped.playlists, key = { "p_" + it.id }) { p ->
                            Text(
                                "☰ " + p.title,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).clickable { vm.push(Screen.Playlist(p.id)) }
                                    .padding(horizontal = 16.dp, vertical = 12.dp).testTag(ElementIds.searchResultPlaylist(p.id)),
                            )
                        }
                    }
                    section(this, "items", itemsTitle, grouped.items) { openHit(vm, it) }
                    if (s.pagingError) {
                        item(key = "paging_retry") {
                            TextButton(onClick = { s.retry() }, modifier = Modifier.padding(16.dp).testTag(ElementIds.SEARCH_PAGING_RETRY_BUTTON)) {
                                Text(t("action.retry"))
                            }
                        }
                    } else if (s.loading) {
                        item(key = "paging_loading") { Box(Modifier.fillMaxWidth().padding(16.dp), Alignment.Center) { CircularProgressIndicator() } }
                    }
                }
            }
        }
    }
}

private fun section(scope: LazyListScope, id: String, title: String, hits: List<SearchHit>, open: (SearchHit) -> Unit) {
    if (hits.isEmpty()) return
    scope.item(key = "h_$id") { SectionTitle(title, ElementIds.searchSection(id)) }
    scope.items(hits, key = { "${id}_" + it.identifier }) { hit -> HitRow(hit, ElementIds.searchResult(hit.identifier)) { open(hit) } }
}

@Composable
fun ErrorState(tag: String, message: String, retryTag: String, onRetry: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(24.dp).testTag(tag), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(message, style = MaterialTheme.typography.bodyLarge)
        Button(onClick = onRetry, modifier = Modifier.padding(top = 12.dp).heightIn(min = 48.dp).testTag(retryTag)) { Text(t("action.retry")) }
    }
}

@Composable
fun BackBar(tag: String, title: String, titleTag: String, onBack: () -> Unit, actions: @Composable () -> Unit = {}) {
    Row(Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        GlyphButton(tag, "←", t("action.back"), onBack)
        Text(title, style = MaterialTheme.typography.titleLarge, maxLines = 2, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f).testTag(titleTag))
        actions()
    }
}

/** Category page: title, sub-category chips and the category's items (UI_SEARCH_SPEC.md → Category page). */
@Composable
fun CategoryScreen(vm: AppViewModel, categoryId: String) {
    val category = vm.repo.categoryMap.find(categoryId) ?: return
    val lang = vm.settings?.appLanguage ?: "en"
    var hits by remember(categoryId) { mutableStateOf<List<SearchHit>?>(null) }
    var failed by remember(categoryId) { mutableStateOf(false) }
    var attempt by remember(categoryId) { mutableStateOf(0) }
    val showAdult = vm.settings?.showAdultContent == true
    LaunchedEffect(categoryId, attempt) {
        failed = false
        val request = SearchRequest("", kind = when (category.kind) { MediaKind.AUDIO -> KindFilter.AUDIO; MediaKind.VIDEO -> KindFilter.VIDEO; null -> KindFilter.ALL },
            category = category.query, sort = SortOrder.POPULARITY)
        hits = runCatching { vm.repo.search(request, showAdult).hits }.onFailure { failed = true }.getOrNull()
    }
    Column(Modifier.fillMaxSize().testTag(ElementIds.CATEGORY_SCREEN)) {
        BackBar(ElementIds.CATEGORY_BACK_BUTTON, category.name(lang), ElementIds.CATEGORY_TITLE, { vm.back() })
        if (category.children.isNotEmpty()) {
            Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                category.children.forEach { c ->
                    SelectChip(ElementIds.categorySubcategory(c.id), c.name(lang), false) { vm.push(Screen.Category(c.id)) }
                }
            }
        }
        when {
            failed -> ErrorState(ElementIds.SEARCH_ERROR, t("search.error"), ElementIds.SEARCH_RETRY_BUTTON) { attempt++ }
            hits == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            else -> LazyColumn(Modifier.fillMaxSize().testTag(ElementIds.SEARCH_RESULTS)) {
                items(hits.orEmpty(), key = { it.identifier }) { hit -> HitRow(hit, ElementIds.searchResult(hit.identifier)) { openHit(vm, hit) } }
            }
        }
    }
}
