package com.jutlandiatech.archiveplayer.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.jutlandiatech.archiveplayer.AppViewModel
import com.jutlandiatech.archiveplayer.Screen
import com.jutlandiatech.archiveplayer.core.home.CuratedRanking
import com.jutlandiatech.archiveplayer.core.home.HistoryRules
import com.jutlandiatech.archiveplayer.core.home.HomeInput
import com.jutlandiatech.archiveplayer.core.home.HomeRow
import com.jutlandiatech.archiveplayer.core.home.HomeRowType
import com.jutlandiatech.archiveplayer.core.home.HomeRows
import com.jutlandiatech.archiveplayer.core.home.Recommender

private fun rowId(row: HomeRow): String = when (row.type) {
    HomeRowType.CONTINUE -> ElementIds.HOME_ROW_CONTINUE
    HomeRowType.RECENTLY_PLAYED -> ElementIds.HOME_ROW_RECENT
    HomeRowType.PLAYLISTS -> ElementIds.HOME_ROW_PLAYLISTS
    HomeRowType.DOWNLOADS -> ElementIds.HOME_ROW_DOWNLOADS
    HomeRowType.NEW_IN_FOLLOWED -> ElementIds.HOME_ROW_NEW_FOLLOWED
    HomeRowType.BECAUSE_YOU_PLAYED -> ElementIds.HOME_ROW_BECAUSE
    HomeRowType.CURATED -> ElementIds.homeCategoryRow(row.categoryId.orEmpty())
}

/** Home tab (UI_HOME_SPEC.md): personal rows first, then one curated row per top-level category. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeTab(vm: AppViewModel) {
    val lib = vm.library.state
    val settings = vm.settings
    val lang = settings?.appLanguage ?: "en"
    LaunchedEffect(Unit) { vm.loadHome() }
    LaunchedEffect(lib.follows) { vm.refreshFollowed(manual = false) }
    val history = if (settings?.historyPaused == true) emptyList() else lib.history
    val continueRow = HistoryRules.continueRow(history)
    val recent = history.sortedByDescending { it.playedAtMs }.distinctBy { it.itemIdentifier }
    val playlists = listOf(lib.liked) + lib.playlists.filterNot { it.isLiked }
    val hasPlaylists = lib.liked.entries.isNotEmpty() || playlists.size > 1
    val because = Recommender.becauseYouPlayed(history, vm.homeRowHits.values.flatten().distinctBy { it.identifier })
    val roots = vm.repo.categoryMap.roots
    val downloaded = vm.downloads.records.filter { it.state == com.jutlandiatech.archiveplayer.data.DownloadState.DONE }.distinctBy { it.identifier }
    val rows = HomeRows.build(
        HomeInput(
            continueCount = continueRow.size, recentCount = recent.size, playlistCount = if (hasPlaylists) playlists.size else 0,
            downloadCount = downloaded.size, newInFollowedCount = vm.newInFollowed.size, becauseYouPlayedCount = because?.items?.size ?: 0,
            curatedCategoryIds = roots.map { it.id },
        ),
    )
    val preferred = settings?.contentLanguages.orEmpty()
    PullToRefreshBox(isRefreshing = vm.homeRefreshing, onRefresh = { vm.loadHome(force = true) }, modifier = Modifier.fillMaxSize().testTag(ElementIds.HOME_SCREEN)) {
        LazyColumn(Modifier.fillMaxSize().testTag(ElementIds.HOME_LIST), state = vm.homeListState) {
            items(rows, key = { rowId(it) }) { row ->
                val id = rowId(row)
                when (row.type) {
                    HomeRowType.CONTINUE -> HomeRowView(id, t("home.row.continue")) {
                        items(continueRow, key = { it.ref.identifier + "/" + it.ref.fileName }) { e ->
                            val left = ((e.durationMs - e.positionMs).coerceAtLeast(0) / 60_000).toString()
                            HomeCard(ElementIds.homeCard(id, e.itemIdentifier), e.ref.title ?: e.itemIdentifier, t("home.card.progress", "minutes" to left),
                                progress = if (e.durationMs > 0) e.positionMs.toFloat() / e.durationMs else 0f,
                                progressTag = ElementIds.homeCardProgress(e.itemIdentifier)) { vm.resume(e) }
                        }
                    }
                    HomeRowType.RECENTLY_PLAYED -> HomeRowView(id, t("home.row.recent")) {
                        items(recent, key = { it.itemIdentifier }) { e ->
                            HomeCard(ElementIds.homeCard(id, e.itemIdentifier), e.itemTitle, e.creator) { vm.push(Screen.Item(e.itemIdentifier)) }
                        }
                    }
                    HomeRowType.PLAYLISTS -> HomeRowView(id, t("home.row.playlists")) {
                        items(playlists, key = { it.id }) { p ->
                            HomeCard(ElementIds.homePlaylist(p.id), playlistTitle(p), p.entries.size.toString()) { vm.push(Screen.Playlist(p.id)) }
                        }
                    }
                    HomeRowType.DOWNLOADS -> HomeRowView(id, t("home.row.downloads")) {
                        items(downloaded, key = { it.identifier }) { r ->
                            HomeCard(ElementIds.homeCard(id, r.identifier), r.itemTitle.ifBlank { r.identifier }, null) { vm.push(Screen.Item(r.identifier)) }
                        }
                    }
                    HomeRowType.NEW_IN_FOLLOWED -> HomeRowView(id, t("home.row.newFollowed")) {
                        items(vm.newInFollowed, key = { it.identifier }) { hit ->
                            HomeCard(ElementIds.homeCard(id, hit.identifier), hit.title, hit.creator) { openHit(vm, hit) }
                        }
                    }
                    HomeRowType.BECAUSE_YOU_PLAYED -> HomeRowView(id, t("home.row.because", "title" to because?.seedTitle.orEmpty())) {
                        items(because?.items.orEmpty(), key = { it.identifier }) { hit ->
                            HomeCard(ElementIds.homeCard(id, hit.identifier), hit.title, hit.creator) { openHit(vm, hit) }
                        }
                    }
                    HomeRowType.CURATED -> {
                        val category = roots.first { it.id == row.categoryId }
                        val status = vm.homeRowStatus[id]
                        // Stale cards are hidden while a row reloads or fails: a LazyRow keeps its first keyed card in place,
                        // which would push a prepended status item out of view.
                        val hits = if (status == null || status == "ok") vm.homeRowHits[id]?.let { CuratedRanking.rank(it, preferred) }.orEmpty() else emptyList()
                        HomeRowView(id, category.name(lang)) {
                            if (status == "loading" || status == "slow") item(key = "loading") {
                                Column(Modifier.width(160.dp).height(160.dp).padding(8.dp).testTag(ElementIds.homeRowLoading(id)), verticalArrangement = Arrangement.Center) {
                                    CircularProgressIndicator()
                                    if (status == "slow") Text(t("home.row.stillLoading"), style = MaterialTheme.typography.bodySmall)
                                }
                            }
                            if (status == "error") item(key = "error") {
                                Text(t("home.row.error"), color = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.padding(8.dp).tagged(ElementIds.homeRowError(id), t("home.row.error")))
                            }
                            if (status == "error" || status == "slow") item(key = "retry") {
                                TextButton(onClick = { vm.loadHomeRow(category.id) }, modifier = Modifier.heightIn(min = 48.dp).testTag(ElementIds.homeRowRetry(id))) { Text(t("action.retry")) }
                            }
                            items(hits, key = { it.identifier }) { hit ->
                                HomeCard(ElementIds.homeCard(id, hit.identifier), hit.title, hit.creator) { openHit(vm, hit) }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeRowView(id: String, title: String, content: LazyListScope.() -> Unit) {
    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 16.dp).tagged(ElementIds.homeRowTitle(id), title))
        LazyRow(Modifier.fillMaxWidth().testTag(ElementIds.homeRow(id)), contentPadding = PaddingValues(horizontal = 8.dp), content = content)
    }
}

@Composable
private fun HomeCard(tag: String, title: String, subtitle: String?, progress: Float? = null, progressTag: String? = null, onClick: () -> Unit) {
    Column(Modifier.width(144.dp).clickable(onClick = onClick).testTag(tag).padding(8.dp)) {
        Box(Modifier.size(128.dp).background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) { Text("♪") }
        if (progress != null && progressTag != null) {
            LinearProgressIndicator(progress = { progress.coerceIn(0f, 1f) }, modifier = Modifier.width(128.dp).testTag(progressTag))
        }
        Text(title, style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
        subtitle?.let { Text(it, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis) }
    }
}
