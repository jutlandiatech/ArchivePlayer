package com.thomasnorlund.archiveplayer.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.thomasnorlund.archiveplayer.Screen
import com.thomasnorlund.archiveplayer.AppViewModel
import com.thomasnorlund.archiveplayer.core.catalog.ArchiveItem
import com.thomasnorlund.archiveplayer.core.catalog.Entry
import com.thomasnorlund.archiveplayer.core.catalog.HtmlSanitizer
import com.thomasnorlund.archiveplayer.core.catalog.MediaKind
import com.thomasnorlund.archiveplayer.core.catalog.SearchHit
import com.thomasnorlund.archiveplayer.core.category.CategoryQuery
import com.thomasnorlund.archiveplayer.core.format.FormatSelector
import com.thomasnorlund.archiveplayer.core.language.LanguageNormalizer
import com.thomasnorlund.archiveplayer.core.playlist.EntryRef
import com.thomasnorlund.archiveplayer.core.playlist.Playlist
import com.thomasnorlund.archiveplayer.core.search.KindFilter
import com.thomasnorlund.archiveplayer.core.search.SearchRequest
import java.util.UUID

fun Entry.ref(): EntryRef = EntryRef(identifier, formats.firstOrNull()?.fileName.orEmpty(), title)

val ArchiveItem.playableEntries: List<Entry>
    get() = entries.filter { e -> e.formats.any { FormatSelector.isLocallyPlayable(it) } }

@Composable
fun ToggleAction(tag: String, label: String, on: Boolean, onToggle: (Boolean) -> Unit) {
    val colors = MaterialTheme.colorScheme
    Surface(
        shape = MaterialTheme.shapes.small,
        color = if (on) colors.secondaryContainer else colors.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, colors.outline),
        modifier = Modifier.heightIn(min = 48.dp)
            .toggleable(value = on, role = Role.Switch, onValueChange = onToggle)
            .tagged(tag, label),
    ) {
        Box(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), contentAlignment = Alignment.Center) { Text(label) }
    }
}

fun shareText(context: android.content.Context, text: String) {
    val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text)
    context.startActivity(Intent.createChooser(send, null).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}

@Composable
private fun TaggedText(tag: String, text: String, style: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.bodyMedium) {
    Text(text, style = style, modifier = Modifier.tagged(tag, text))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemScreen(vm: AppViewModel, identifier: String) {
    var item by remember(identifier) { mutableStateOf<ArchiveItem?>(null) }
    var failed by remember(identifier) { mutableStateOf(false) }
    var attempt by remember(identifier) { mutableIntStateOf(0) }
    LaunchedEffect(identifier, attempt) {
        failed = false
        item = runCatching { vm.repo.metadata(identifier) }
            .onFailure { android.util.Log.w("ArchivePlayer", "metadata failed: ${it.message}") }
            .getOrNull()
        failed = item == null
    }
    Column(Modifier.fillMaxSize().testTag(ElementIds.ITEM_SCREEN)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            GlyphButton(ElementIds.ITEM_BACK_BUTTON, "←", t("action.back")) { vm.back() }
        }
        val loaded = item
        when {
            failed -> ErrorState(ElementIds.ITEM_ERROR, t("item.error"), ElementIds.ITEM_RETRY_BUTTON) { attempt++ }
            loaded == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            else -> ItemBody(vm, loaded)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun ItemBody(vm: AppViewModel, item: ArchiveItem) {
    val context = LocalContext.current
    val lib = vm.library.state
    val playable = remember(item) { item.playableEntries }
    var filter by rememberSaveable(item.identifier) { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    var menuFor by remember { mutableStateOf<Int?>(null) }
    var pickerFor by remember { mutableStateOf<Int?>(null) }
    var newPlaylistFor by remember { mutableStateOf<Int?>(null) }
    val followId = item.collections.firstOrNull() ?: item.identifier
    val liked = lib.liked.entries.any { it.identifier == item.identifier }
    val following = followId in lib.follows
    val isAudio = item.kind == MediaKind.AUDIO
    val shown = playable.withIndex().filter { filter.isBlank() || it.value.title.contains(filter.trim(), ignoreCase = true) }

    val listState = rememberLazyListState()
    LaunchedEffect(filter) { if (filter.isNotBlank()) listState.scrollToItem(1) }
    LazyColumn(Modifier.fillMaxSize().testTag(ElementIds.ITEM_ENTRIES), state = listState, contentPadding = PaddingValues(16.dp)) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                TaggedText(ElementIds.ITEM_TITLE, item.title, MaterialTheme.typography.headlineSmall)
                item.creator?.let { TaggedText(ElementIds.ITEM_CREATOR, it) }
                item.date?.take(4)?.let { TaggedText(ElementIds.ITEM_YEAR, it) }
                TaggedText(ElementIds.ITEM_KIND, t("search.filter.kind." + item.kind.name.lowercase()))
                if (item.languages.isNotEmpty()) {
                    TaggedText(ElementIds.ITEM_LANGUAGES, item.languages.sorted().joinToString(", ") { "${LanguageNormalizer.nativeName(it)} ($it)" })
                }
                item.collections.firstOrNull()?.let { c ->
                    TextButton(onClick = { vm.push(Screen.Collection(c)) }, modifier = Modifier.tagged(ElementIds.ITEM_COLLECTION_LINK, c)) { Text(c) }
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { vm.playEntries(item, playable, 0) }, enabled = playable.isNotEmpty(),
                        modifier = Modifier.heightIn(min = 48.dp).testTag(ElementIds.ITEM_PLAY_ALL_BUTTON)) { Text(t("item.playAll")) }
                    OutlinedButton(onClick = { vm.playEntries(item, playable, 0, shuffle = true) }, enabled = playable.isNotEmpty(),
                        modifier = Modifier.heightIn(min = 48.dp).testTag(ElementIds.ITEM_SHUFFLE_BUTTON)) { Text(t("item.shuffle")) }
                    ToggleAction(ElementIds.ITEM_LIKE_BUTTON, t(if (liked) "item.liked" else "item.like"), liked) { on ->
                        vm.setLiked(item, playable, on)
                    }
                    ToggleAction(ElementIds.ITEM_FOLLOW_BUTTON, t(if (following) "item.following" else "item.follow"), following) { on ->
                        vm.setFollowing(followId, on)
                    }
                    OutlinedButton(onClick = { shareText(context, item.title + "\n" + item.archiveUrl) },
                        modifier = Modifier.heightIn(min = 48.dp).testTag(ElementIds.ITEM_SHARE_BUTTON)) { Text(t("item.share")) }
                    if (isAudio && playable.isNotEmpty()) {
                        val label = t(vm.downloadLabelKey(item))
                        OutlinedButton(onClick = { vm.downloadItem(item) },
                            modifier = Modifier.heightIn(min = 48.dp).tagged(ElementIds.ITEM_DOWNLOAD_BUTTON, label)) { Text(label) }
                    }
                }
                item.description?.let { HtmlSanitizer.toPlainText(it) }?.takeIf { it.isNotBlank() }?.let { d ->
                    Text(d, maxLines = if (expanded) Int.MAX_VALUE else 4, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.testTag(ElementIds.ITEM_DESCRIPTION))
                    TextButton(onClick = { expanded = !expanded }, modifier = Modifier.testTag(ElementIds.ITEM_DESCRIPTION_TOGGLE)) {
                        Text(t(if (expanded) "item.less" else "item.more"))
                    }
                }
                if (playable.isEmpty()) {
                    TaggedText(ElementIds.ITEM_NOTHING_PLAYABLE, t("item.nothingPlayable"))
                    Button(onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(item.archiveUrl)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) },
                        modifier = Modifier.testTag(ElementIds.ITEM_OPEN_ARCHIVE_BUTTON)) { Text(t("item.openArchive")) }
                }
            }
        }
        if (playable.isNotEmpty()) stickyHeader {
            Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(vertical = 8.dp)) {
                    if (playable.size > 30) {
                        OutlinedTextField(filter, { filter = it }, label = { Text(t("item.filter")) }, singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag(ElementIds.ITEM_ENTRY_FILTER))
                    }
                    Text(t("item.entries", "count" to playable.size.toString()), style = MaterialTheme.typography.titleSmall)
                }
            }
        }
        items(shown, key = { it.index }) { (i, entry) ->
            Row(Modifier.fillMaxWidth().heightIn(min = 56.dp), verticalAlignment = Alignment.CenterVertically) {
                val duration = entry.durationSeconds?.let { s -> "%d:%02d".format((s / 60).toInt(), (s % 60).toInt()) }.orEmpty()
                Box(Modifier.weight(1f).heightIn(min = 56.dp).clickable { vm.playEntries(item, playable, i) }
                    .tagged(ElementIds.itemEntryRow(i), listOf(entry.title, duration).filter { it.isNotEmpty() }.joinToString(" · ")),
                    contentAlignment = Alignment.CenterStart) {
                    Column {
                        Text(entry.title, style = MaterialTheme.typography.bodyLarge)
                        if (duration.isNotEmpty()) Text(duration, style = MaterialTheme.typography.bodySmall)
                    }
                }
                GlyphButton(ElementIds.itemEntryMenu(i), "⋮", t("item.more")) { menuFor = i }
            }
        }
    }

    menuFor?.let { i ->
        val entry = playable[i]
        val entryLiked = lib.liked.entries.any { it == entry.ref() }
        ModalBottomSheet(onDismissRequest = { menuFor = null }, modifier = Modifier.testTag(ElementIds.SHEET_ENTRY)) {
            @Composable fun action(key: String, label: String, run: () -> Unit) {
                Box(Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable { menuFor = null; run() }
                    .tagged(ElementIds.entryMenu(key), label).padding(horizontal = 24.dp), contentAlignment = Alignment.CenterStart) { Text(label) }
            }
            action("play_next", t("entry.playNext")) { vm.enqueue(item, entry, next = true) }
            action("add_to_queue", t("entry.addToQueue")) { vm.enqueue(item, entry, next = false) }
            action("add_to_playlist", t("entry.addToPlaylist")) { pickerFor = i }
            if (entry.kind == MediaKind.AUDIO) action("download", t("entry.download")) { vm.downloadEntry(item, entry) }
            action(if (entryLiked) "unlike" else "like", t(if (entryLiked) "entry.unlike" else "entry.like")) { vm.setEntryLiked(entry, !entryLiked) }
            action("share", t("entry.share")) { shareText(context, entry.title + "\n" + item.archiveUrl) }
            Spacer(Modifier.height(24.dp))
        }
    }

    pickerFor?.let { i ->
        ModalBottomSheet(onDismissRequest = { pickerFor = null }, modifier = Modifier.testTag(ElementIds.SHEET_PLAYLIST_PICKER)) {
            Box(Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable { pickerFor = null; newPlaylistFor = i }
                .tagged(ElementIds.PLAYLIST_PICKER_NEW, t("library.newPlaylist")).padding(horizontal = 24.dp), contentAlignment = Alignment.CenterStart) {
                Text(t("library.newPlaylist"))
            }
            lib.playlists.filterNot { it.isLiked }.forEach { p ->
                Box(Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable { pickerFor = null; vm.addToPlaylist(p.id, listOf(playable[i].ref())) }
                    .tagged(ElementIds.playlistPicker(p.id), p.title).padding(horizontal = 24.dp), contentAlignment = Alignment.CenterStart) { Text(p.title) }
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    newPlaylistFor?.let { i -> NewPlaylistDialog(onDismiss = { newPlaylistFor = null }) { name -> vm.createPlaylist(name, listOf(playable[i].ref())) } }
}

@Composable
fun NewPlaylistDialog(onDismiss: () -> Unit, onCreate: (String) -> Unit) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(t("library.newPlaylist")) },
        text = {
            OutlinedTextField(name, { name = it }, label = { Text(t("library.newPlaylist.name")) }, singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag(ElementIds.DIALOG_PLAYLIST_NAME_FIELD))
        },
        confirmButton = {
            TextButton(onClick = { onDismiss(); onCreate(name.trim()) }, enabled = name.isNotBlank(),
                modifier = Modifier.testTag(ElementIds.DIALOG_CONFIRM_BUTTON)) { Text(t("action.save")) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.testTag(ElementIds.DIALOG_CANCEL_BUTTON)) { Text(t("action.cancel")) }
        },
    )
}

@Composable
fun CollectionScreen(vm: AppViewModel, identifier: String) {
    var kind by rememberSaveable(identifier) { mutableStateOf(KindFilter.ALL) }
    var hits by remember(identifier) { mutableStateOf<List<SearchHit>?>(null) }
    var title by remember(identifier) { mutableStateOf<String?>(null) }
    var failed by remember(identifier) { mutableStateOf(false) }
    var attempt by remember(identifier) { mutableIntStateOf(0) }
    val showAdult = vm.settings?.showAdultContent == true
    val context = LocalContext.current
    LaunchedEffect(identifier) { title = runCatching { vm.repo.metadata(identifier) }.getOrNull()?.title ?: identifier }
    LaunchedEffect(identifier, kind, attempt) {
        failed = false
        hits = null
        runCatching {
            vm.repo.search(SearchRequest("", kind, CategoryQuery(listOf(identifier), emptyList(), null), rows = 100), showAdult)
        }.onSuccess { page -> hits = page.hits.filter { it.identifier != identifier } }.onFailure { failed = true }
    }
    val following = identifier in vm.library.state.follows
    val shownTitle = title ?: return Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
    Column(Modifier.fillMaxSize().testTag(ElementIds.COLLECTION_SCREEN)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            GlyphButton(ElementIds.COLLECTION_BACK_BUTTON, "←", t("action.back")) { vm.back() }
            Text(shownTitle, style = MaterialTheme.typography.titleLarge, maxLines = 2, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f).tagged(ElementIds.COLLECTION_TITLE, shownTitle))
        }
        Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            ToggleAction(ElementIds.COLLECTION_FOLLOW_BUTTON, t(if (following) "item.following" else "item.follow"), following) { on ->
                vm.setFollowing(identifier, on)
            }
            OutlinedButton(onClick = { shareText(context, "$shownTitle\nhttps://archive.org/details/$identifier") },
                modifier = Modifier.heightIn(min = 48.dp).testTag(ElementIds.COLLECTION_SHARE_BUTTON)) { Text(t("item.share")) }
            KindFilter.entries.forEach { k ->
                val key = k.name.lowercase()
                SelectChip(ElementIds.searchFilterKind(key), t("search.filter.kind.$key"), kind == k) { kind = k }
            }
        }
        val list = hits
        when {
            failed -> ErrorState(ElementIds.ITEM_ERROR, t("item.error"), ElementIds.ITEM_RETRY_BUTTON) { attempt++ }
            list == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            else -> {
                TaggedText(ElementIds.COLLECTION_COUNT, t("collection.items", "count" to list.size.toString()))
                LazyColumn(Modifier.fillMaxSize().testTag(ElementIds.COLLECTION_ITEMS)) {
                    items(list, key = { it.identifier }) { hit ->
                        HitRow(hit, ElementIds.collectionItem(hit.identifier)) {
                            vm.push(if (hit.kind == null) Screen.Collection(hit.identifier) else Screen.Item(hit.identifier))
                        }
                    }
                }
            }
        }
    }
}
