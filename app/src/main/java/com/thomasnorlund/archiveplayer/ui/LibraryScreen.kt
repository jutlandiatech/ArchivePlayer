package com.thomasnorlund.archiveplayer.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.thomasnorlund.archiveplayer.AppViewModel
import com.thomasnorlund.archiveplayer.Screen
import com.thomasnorlund.archiveplayer.core.playlist.EntryRef
import com.thomasnorlund.archiveplayer.core.playlist.Playlist
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

@Composable
private fun RowLabel(tag: String, label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier.fillMaxWidth().heightIn(min = 56.dp).clickable(onClick = onClick).tagged(tag, label).padding(horizontal = 16.dp),
        contentAlignment = Alignment.CenterStart,
    ) { Text(label, style = MaterialTheme.typography.bodyLarge) }
}

@Composable
fun playlistTitle(p: Playlist) = if (p.isLiked) t("library.liked") else p.title

private fun EntryRef.label() = title ?: fileName

@Composable
fun ConfirmDialog(message: String, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        text = { Text(message) },
        confirmButton = { TextButton(onClick = { onDismiss(); onConfirm() }, modifier = Modifier.testTag(ElementIds.DIALOG_CONFIRM_BUTTON)) { Text(t("action.ok")) } },
        dismissButton = { TextButton(onClick = onDismiss, modifier = Modifier.testTag(ElementIds.DIALOG_CANCEL_BUTTON)) { Text(t("action.cancel")) } },
    )
}

@Composable
fun LibraryTab(vm: AppViewModel) {
    val lib = vm.library.state
    val filter = vm.libraryFilter
    var newPlaylist by remember { mutableStateOf(false) }
    var confirmClear by remember { mutableStateOf(false) }
    var confirmRemoveAll by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().testTag(ElementIds.LIBRARY_SCREEN)) {
        Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ElementIds.LIBRARY_FILTERS.forEach { f -> SelectChip(ElementIds.libraryFilter(f), t("library.filter.$f"), filter == f) { vm.libraryFilter = f } }
        }
        vm.deletedPlaylist?.let { deleted ->
            LaunchedEffect(deleted) { delay(5_000); vm.dismissUndo() }
            TextButton(onClick = { vm.undoDelete() }, modifier = Modifier.padding(horizontal = 16.dp).heightIn(min = 48.dp)
                .tagged(ElementIds.PLAYLIST_UNDO_DELETE, t("playlist.deleted.undo"))) { Text(t("playlist.deleted.undo")) }
        }
        LazyColumn(Modifier.fillMaxSize().testTag(ElementIds.LIBRARY_LIST)) {
            @Suppress("NAME_SHADOWING")
            fun empty(filter: String) = item { Box(Modifier.padding(24.dp)) { EmptyText("library.empty.$filter") } }
            when (filter) {
                "playlists" -> {
                    item {
                        Button(onClick = { newPlaylist = true }, modifier = Modifier.padding(16.dp).heightIn(min = 48.dp)
                            .testTag(ElementIds.LIBRARY_NEW_PLAYLIST_BUTTON)) { Text(t("library.newPlaylist")) }
                    }
                    items(listOf(lib.liked) + lib.playlists.filterNot { it.isLiked }, key = { it.id }) { p ->
                        RowLabel(ElementIds.libraryPlaylist(p.id), playlistTitle(p) + " · " + t("library.entries", "count" to p.entries.size.toString())) {
                            vm.push(Screen.Playlist(p.id))
                        }
                    }
                }
                "liked" -> if (lib.liked.entries.isEmpty()) empty(filter) else itemsIndexed(lib.liked.entries) { i, ref ->
                    RowLabel(ElementIds.playlistEntryRow(i), ref.label()) { vm.playRefs(lib.liked.entries, i) }
                }
                "following" -> if (lib.follows.isEmpty()) empty(filter) else items(lib.follows, key = { it }) { id ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RowLabel(ElementIds.libraryFollowing(id), id, Modifier.weight(1f)) { vm.push(Screen.Collection(id)) }
                        TextButton(onClick = { vm.setFollowing(id, false) }, modifier = Modifier.heightIn(min = 48.dp)
                            .testTag(ElementIds.libraryUnfollow(id))) { Text(t("library.unfollow")) }
                    }
                }
                "downloads" -> {
                    val records = vm.downloads.records
                    val location = vm.settings?.downloadLocation ?: com.thomasnorlund.archiveplayer.core.settings.DownloadLocation.INTERNAL
                    val free = vm.downloads.freeBytes(location)
                    item {
                        Column(Modifier.padding(16.dp)) {
                            val summary = t("library.storage", "used" to formatBytes(vm.downloads.usedBytes()), "free" to formatBytes(free))
                            Text(summary, modifier = Modifier.tagged(ElementIds.LIBRARY_STORAGE_SUMMARY, summary))
                            if (free < com.thomasnorlund.archiveplayer.core.download.StoragePolicy.lowSpaceThresholdBytes) {
                                Text(t("library.storage.low"), color = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.tagged(ElementIds.LIBRARY_STORAGE_WARNING, t("library.storage.low")))
                            }
                        }
                    }
                    if (records.isEmpty()) empty(filter) else {
                        item {
                            TextButton(onClick = { confirmRemoveAll = true }, modifier = Modifier.padding(horizontal = 8.dp).heightIn(min = 48.dp)
                                .testTag(ElementIds.LIBRARY_DOWNLOADS_REMOVE_ALL)) { Text(t("library.downloads.removeAll")) }
                        }
                        items(records.groupBy { it.identifier }.toList(), key = { it.first }) { (identifier, list) ->
                            val waiting = list.any { it.state == com.thomasnorlund.archiveplayer.data.DownloadState.WAITING_WIFI }
                            val size = list.sumOf { it.sizeBytes }
                            Column(Modifier.padding(horizontal = 8.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    RowLabel(ElementIds.libraryDownload(identifier), list.first().itemTitle.ifBlank { identifier } + " · " +
                                        t("library.entries", "count" to list.size.toString()) + (if (size > 0) " · " + formatBytes(size) else ""),
                                        Modifier.weight(1f)) { vm.push(Screen.Item(identifier)) }
                                    TagGlyphButton(ElementIds.libraryDownloadRemove(identifier), "✕", t("entry.removeDownload")) { vm.removeDownloads(identifier) }
                                }
                                if (waiting) Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(t("library.downloads.waitingWifi"), style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(start = 8.dp).weight(1f))
                                    TextButton(onClick = { vm.downloads.allowMobile(identifier) }, modifier = Modifier.heightIn(min = 48.dp)
                                        .testTag(ElementIds.libraryDownloadMobileNow(identifier))) { Text(t("library.downloads.useMobile")) }
                                }
                            }
                        }
                    }
                }
                "history" -> {
                    if (vm.settings?.historyPaused == true) item {
                        Text(t("settings.history.pause"), modifier = Modifier.padding(16.dp).tagged(ElementIds.LIBRARY_HISTORY_PAUSED, t("settings.history.pause")))
                    }
                    val events = lib.history.sortedByDescending { it.playedAtMs }
                    if (events.isEmpty()) empty(filter) else {
                        item {
                            TextButton(onClick = { confirmClear = true }, modifier = Modifier.padding(horizontal = 8.dp).heightIn(min = 48.dp)
                                .testTag(ElementIds.LIBRARY_HISTORY_CLEAR)) { Text(t("library.history.clear")) }
                        }
                        itemsIndexed(events) { i, e ->
                            RowLabel(ElementIds.libraryHistory(i), e.ref.label() + " · " + e.itemTitle) { vm.push(Screen.Item(e.itemIdentifier)) }
                        }
                    }
                }
            }
        }
    }
    if (newPlaylist) NewPlaylistDialog(onDismiss = { newPlaylist = false }) { name -> vm.push(Screen.Playlist(vm.createPlaylist(name, emptyList()))) }
    if (confirmClear) ConfirmDialog(t("library.history.clear"), { confirmClear = false }) { vm.clearHistory() }
    if (confirmRemoveAll) ConfirmDialog(t("library.downloads.removeAll.confirm"), { confirmRemoveAll = false }) { vm.downloads.removeAll() }
}

@Composable
private fun EmptyText(key: String) {
    val text = t(key)
    Text(text, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.tagged(ElementIds.LIBRARY_EMPTY, text))
}

@Composable
fun TextDialog(title: String, initial: String, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var value by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { OutlinedTextField(value, { value = it }, modifier = Modifier.fillMaxWidth().testTag(ElementIds.DIALOG_PLAYLIST_NAME_FIELD)) },
        confirmButton = { val keyboard = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
            TextButton(onClick = { keyboard?.hide(); onDismiss(); onSave(value) }, modifier = Modifier.testTag(ElementIds.DIALOG_CONFIRM_BUTTON)) { Text(t("action.save")) } },
        dismissButton = { TextButton(onClick = onDismiss, modifier = Modifier.testTag(ElementIds.DIALOG_CANCEL_BUTTON)) { Text(t("action.cancel")) } },
    )
}

@Composable
fun PlaylistScreen(vm: AppViewModel, playlistId: String) {
    val p = vm.library.state.playlists.firstOrNull { it.id == playlistId } ?: return
    val context = LocalContext.current
    var editing by remember { mutableStateOf(false) }
    var menu by remember { mutableStateOf(false) }
    var dialog by remember { mutableStateOf<String?>(null) }
    val title = playlistTitle(p)
    LaunchedEffect(playlistId) { vm.playlistVideoNotice = 0 }
    val shareContext = androidx.compose.ui.platform.LocalContext.current
    vm.shareTooLong?.let { pl ->
        AlertDialog(
            onDismissRequest = { vm.shareTooLong = null },
            modifier = Modifier.testTag(ElementIds.DIALOG_SHARE_TOO_LONG),
            text = { Text(t("share.tooLong")) },
            confirmButton = {
                TextButton(onClick = { vm.sharePlaylistFile(shareContext, pl) }, modifier = Modifier.heightIn(min = 48.dp).testTag(ElementIds.SHARE_AS_FILE_BUTTON)) { Text(t("share.asFile")) }
            },
            dismissButton = { TextButton(onClick = { vm.shareTooLong = null }, modifier = Modifier.heightIn(min = 48.dp)) { Text(t("action.cancel")) } },
        )
    }
    LazyColumn(Modifier.fillMaxSize().testTag(ElementIds.PLAYLIST_SCREEN)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TagGlyphButton(ElementIds.PLAYLIST_BACK_BUTTON, "←", t("action.back")) { vm.back() }
                Spacer(Modifier.weight(1f))
                TextButton(onClick = { editing = !editing }, modifier = Modifier.heightIn(min = 48.dp).testTag(ElementIds.PLAYLIST_EDIT_BUTTON)) { Text(t("playlist.edit")) }
                Box {
                    TagGlyphButton(ElementIds.PLAYLIST_OVERFLOW_BUTTON, "⋮", t("item.more")) { menu = true }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        val actions = if (p.isLiked) listOf("export") else listOf("rename", "description", "export", "delete")
                        actions.forEach { a ->
                            DropdownMenuItem(text = { Text(t("playlist.$a")) }, modifier = Modifier.testTag(ElementIds.playlistMenu(a)), onClick = {
                                menu = false
                                when (a) {
                                    "delete" -> { vm.deletePlaylist(p.id); vm.back() }
                                    "export" -> Unit
                                    else -> dialog = a
                                }
                            })
                        }
                    }
                }
            }
        }
        item {
            Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.tagged(ElementIds.PLAYLIST_TITLE, title))
                p.description?.let { Text(it, modifier = Modifier.tagged(ElementIds.PLAYLIST_DESCRIPTION, it)) }
                val count = t("library.entries", "count" to p.entries.size.toString())
                Text(count, style = MaterialTheme.typography.bodySmall, modifier = Modifier.tagged(ElementIds.PLAYLIST_COUNT, count))
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { vm.playRefs(p.entries, 0) }, enabled = p.entries.isNotEmpty(),
                        modifier = Modifier.heightIn(min = 48.dp).testTag(ElementIds.PLAYLIST_PLAY_BUTTON)) { Text(t("playlist.play")) }
                    OutlinedButton(onClick = { vm.playRefs(p.entries, 0, shuffle = true) }, enabled = p.entries.isNotEmpty(),
                        modifier = Modifier.heightIn(min = 48.dp).testTag(ElementIds.PLAYLIST_SHUFFLE_BUTTON)) { Text(t("playlist.shuffle")) }
                    OutlinedButton(onClick = { vm.sharePlaylist(context, p) },
                        modifier = Modifier.heightIn(min = 48.dp).testTag(ElementIds.PLAYLIST_SHARE_BUTTON)) { Text(t("playlist.share")) }
                    OutlinedButton(onClick = { vm.downloadPlaylist(p) },
                        modifier = Modifier.heightIn(min = 48.dp).testTag(ElementIds.PLAYLIST_DOWNLOAD_BUTTON)) { Text(t("playlist.download")) }
                }
                if (vm.playlistVideoNotice > 0) {
                    val notice = t("playlist.videoNotDownloaded", "count" to vm.playlistVideoNotice.toString())
                    Text(notice, style = MaterialTheme.typography.bodySmall, modifier = Modifier.tagged(ElementIds.PLAYLIST_VIDEO_NOTICE, notice))
                }
            }
        }
        itemsIndexed(p.entries) { i, ref ->
            var rowHeight by remember { mutableIntStateOf(0) }
            Row(Modifier.onSizeChanged { rowHeight = it.height }, verticalAlignment = Alignment.CenterVertically) {
                if (editing) {
                    var dy by remember { mutableFloatStateOf(0f) }
                    Box(Modifier.size(48.dp).pointerInput(i, p.entries.size) {
                        val row = rowHeight.takeIf { it > 0 }?.toFloat() ?: 56.dp.toPx()
                        detectVerticalDragGestures(
                            onDragStart = { dy = 0f },
                            onDragEnd = {
                                val target = (i + (dy / row).roundToInt()).coerceIn(0, p.entries.lastIndex)
                                if (target != i) vm.reorderPlaylist(p.id, i, target)
                            },
                        ) { _, delta -> dy += delta }
                    }.tagged(ElementIds.playlistEntryDrag(i), "≡"), contentAlignment = Alignment.Center) { Text("≡") }
                }
                RowLabel(ElementIds.playlistEntryRow(i), ref.label(), Modifier.weight(1f)) { vm.playRefs(p.entries, i) }
                if (editing) TagGlyphButton(ElementIds.playlistEntryRemove(i), "✕", t("playlist.remove")) { vm.removeFromPlaylist(p.id, i) }
            }
        }
    }
    when (dialog) {
        "rename" -> TextDialog(t("playlist.rename"), p.title, { dialog = null }) { vm.renamePlaylist(p.id, it.trim()) }
        "description" -> TextDialog(t("playlist.description"), p.description.orEmpty(), { dialog = null }) { vm.describePlaylist(p.id, it) }
    }
}

fun formatBytes(bytes: Long): String = when {
    bytes >= 1_000_000_000L -> "%.1f GB".format(bytes / 1e9)
    bytes >= 1_000_000L -> "%.0f MB".format(bytes / 1e6)
    else -> "%.0f kB".format(bytes / 1e3)
}

@Composable
fun ImportScreen(vm: AppViewModel) {
    val p = vm.pendingImport
    LazyColumn(Modifier.fillMaxSize().testTag(ElementIds.IMPORT_SCREEN)) {
        item { Text(t("import.title"), style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(16.dp)) }
        if (p == null || p.invalid) {
            item { Text(t("import.invalid"), color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(horizontal = 16.dp)) }
            item {
                TextButton(onClick = { vm.cancelImport() }, modifier = Modifier.padding(8.dp).heightIn(min = 48.dp).testTag(ElementIds.IMPORT_CANCEL_BUTTON)) { Text(t("action.cancel")) }
            }
            return@LazyColumn
        }
        item {
            Column(Modifier.padding(horizontal = 16.dp)) {
                Text(p.title, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.tagged(ElementIds.IMPORT_TITLE, p.title))
                p.description?.let { Text(it) }
                val count = t("library.entries", "count" to p.entries.size.toString())
                Text(count, modifier = Modifier.tagged(ElementIds.IMPORT_COUNT, count))
                if (p.skipped > 0) {
                    val skipped = t("import.skipped", "count" to p.skipped.toString())
                    Text(skipped, color = MaterialTheme.colorScheme.error, modifier = Modifier.tagged(ElementIds.IMPORT_SKIPPED, skipped))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 8.dp)) {
                    Button(onClick = { vm.confirmImport() }, modifier = Modifier.heightIn(min = 48.dp).testTag(ElementIds.IMPORT_CONFIRM_BUTTON)) { Text(t("import.confirm")) }
                    OutlinedButton(onClick = { vm.cancelImport() }, modifier = Modifier.heightIn(min = 48.dp).testTag(ElementIds.IMPORT_CANCEL_BUTTON)) { Text(t("action.cancel")) }
                }
            }
        }
        itemsIndexed(p.entries.take(100)) { i, e ->
            RowLabel(ElementIds.importEntry(i), (e.title ?: e.fileName) + " · " + e.identifier) { vm.push(Screen.Item(e.identifier)) }
        }
    }
}
