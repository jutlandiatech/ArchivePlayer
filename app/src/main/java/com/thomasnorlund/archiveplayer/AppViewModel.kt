package com.thomasnorlund.archiveplayer

import com.thomasnorlund.archiveplayer.ui.ref
import android.app.Application
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import com.thomasnorlund.archiveplayer.core.catalog.ArchiveItem
import com.thomasnorlund.archiveplayer.core.catalog.Entry
import com.thomasnorlund.archiveplayer.core.catalog.MediaFormat
import com.thomasnorlund.archiveplayer.core.catalog.MediaKind
import com.thomasnorlund.archiveplayer.core.catalog.SubtitleType
import com.thomasnorlund.archiveplayer.core.format.FormatSelector
import com.thomasnorlund.archiveplayer.core.format.PlaybackConditions
import com.thomasnorlund.archiveplayer.playback.QueueEntry
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.thomasnorlund.archiveplayer.data.ArchiveRepository
import com.thomasnorlund.archiveplayer.data.LibraryStore
import com.thomasnorlund.archiveplayer.ui.SearchController
import com.thomasnorlund.archiveplayer.core.settings.AppSettings
import com.thomasnorlund.archiveplayer.core.settings.ThemeResolver
import com.thomasnorlund.archiveplayer.core.setup.SetupFlow
import com.thomasnorlund.archiveplayer.data.SettingsStore
import com.thomasnorlund.archiveplayer.ui.AppTexts
import java.util.Locale

enum class Tab { HOME, SEARCH, LIBRARY }

/** A destination pushed onto a tab's back stack (UI_SHELL_SPEC.md → Top-level structure). */
sealed interface Screen {
    data object Settings : Screen
    data class SettingsDetail(val row: String) : Screen
    data class Item(val identifier: String) : Screen
    data class Collection(val identifier: String) : Screen
    data class Category(val categoryId: String) : Screen
    data class Playlist(val playlistId: String) : Screen
    data object Import : Screen
}

/** Survives configuration changes and activity recreation; owns settings, setup and navigation state. */
class AppViewModel(application: Application) : AndroidViewModel(application) {
    val texts: AppTexts = AppTexts.load(application)
    private val store = SettingsStore(application)
    val repo = ArchiveRepository(application)
    val library = LibraryStore(application)
    val phoneLanguage: String = Locale.getDefault().toLanguageTag()

    var settings: AppSettings? by mutableStateOf(store.load())
        private set

    val setup = SetupFlow(phoneLanguage, texts.isoCodes)
    /** Bumped on every setup action so Compose re-reads [setup]. */
    var setupVersion by mutableIntStateOf(0)
        private set

    fun onSetup(action: SetupFlow.() -> Unit) {
        setup.action()
        setupVersion++
    }

    fun startApp() {
        val result = setup.start() ?: return
        updateSettings(result)
    }

    fun updateSettings(new: AppSettings) {
        store.save(new)
        settings = new
    }

    fun toggleTheme(systemDark: Boolean) {
        val s = settings ?: return
        updateSettings(s.copy(theme = ThemeResolver.toggle(s.theme, systemDark)))
    }

    val search = SearchController(viewModelScope, repo, library, { settings?.showAdultContent == true }, { settings?.appLanguage ?: "en" })

    var tab by mutableStateOf(Tab.HOME)
        private set
    private val stacks: Map<Tab, SnapshotStateList<Screen>> = Tab.entries.associateWith { mutableStateListOf() }
    fun stack(tab: Tab): List<Screen> = stacks.getValue(tab)
    val current: Screen? get() = stacks.getValue(tab).lastOrNull()

    /** Reselecting the active tab pops to its root (N04). */
    fun selectTab(target: Tab) {
        if (target == tab) stacks.getValue(tab).clear() else tab = target
    }

    fun push(screen: Screen) {
        stacks.getValue(tab).add(screen)
    }

    /** Android/in-app Back: pop within the tab, then return to Home; false when the app should close (N05). */
    fun back(): Boolean {
        val stack = stacks.getValue(tab)
        return when {
            stack.isNotEmpty() -> { stack.removeAt(stack.lastIndex); true }
            tab != Tab.HOME -> { tab = Tab.HOME; true }
            else -> false
        }
    }

    // Playback/downloads are wired by the playback slice; these entry points are the UI contract.
    val downloads = com.thomasnorlund.archiveplayer.data.Downloads(application) { settings?.wifiOnly != false }
    var playlistVideoNotice by mutableStateOf(0)

    private fun downloadLocation(): com.thomasnorlund.archiveplayer.core.settings.DownloadLocation {
        val selected = settings?.downloadLocation ?: com.thomasnorlund.archiveplayer.core.settings.DownloadLocation.INTERNAL
        return if (selected == com.thomasnorlund.archiveplayer.core.settings.DownloadLocation.SD_CARD && !downloads.sdCardMounted())
            com.thomasnorlund.archiveplayer.core.settings.DownloadLocation.INTERNAL else selected
    }

    private fun downloadRecord(item: ArchiveItem, e: Entry): com.thomasnorlund.archiveplayer.data.DownloadRecord? {
        if (e.kind != MediaKind.AUDIO) return null
        val f = FormatSelector.select(e, PlaybackConditions(metered = false, dataSaver = false, forCast = false)) ?: return null
        return com.thomasnorlund.archiveplayer.data.DownloadRecord(item.identifier, e.ref().fileName, f.fileName, e.title, item.title,
            repo.downloadUrl(item.identifier, f.fileName), downloadLocation())
    }

    private fun audioEntries(item: ArchiveItem) = item.entries.filter { it.kind == MediaKind.AUDIO && FormatSelector.select(it, conditions()) != null }

    fun isItemDownloaded(item: ArchiveItem): Boolean {
        val done = downloads.records.filter { it.identifier == item.identifier && it.state == com.thomasnorlund.archiveplayer.data.DownloadState.DONE }.map { it.fileName }.toSet()
        val audio = audioEntries(item)
        return audio.isNotEmpty() && audio.all { it.ref().fileName in done }
    }

    fun isEntryDownloaded(item: ArchiveItem, e: Entry) = downloads.playableFile(item.identifier, e.ref().fileName) != null

    fun downloadItem(item: ArchiveItem) = downloads.enqueue(audioEntries(item).mapNotNull { downloadRecord(item, it) })

    fun downloadEntry(item: ArchiveItem, entry: Entry) {
        if (isEntryDownloaded(item, entry)) downloads.remove { it.identifier == item.identifier && it.fileName == entry.ref().fileName }
        else downloads.enqueue(listOfNotNull(downloadRecord(item, entry)))
    }

    fun removeDownloads(identifier: String) = downloads.remove { it.identifier == identifier }

    fun downloadLabelKey(item: ArchiveItem): String {
        val mine = downloads.records.filter { it.identifier == item.identifier }
        return when {
            isItemDownloaded(item) -> "item.downloaded"
            mine.any { it.state != com.thomasnorlund.archiveplayer.data.DownloadState.DONE && it.state != com.thomasnorlund.archiveplayer.data.DownloadState.FAILED } -> "item.downloading"
            else -> "item.download"
        }
    }

    fun setLiked(item: com.thomasnorlund.archiveplayer.core.catalog.ArchiveItem, entries: List<com.thomasnorlund.archiveplayer.core.catalog.Entry>, on: Boolean) =
        library.update { s ->
            val liked = s.liked
            val kept = liked.entries.filterNot { it.identifier == item.identifier }
            val next = if (on) kept + entries.map { it.ref() } else kept
            s.copy(playlists = s.playlists.map { if (it.isLiked) liked.copy(entries = next) else it })
        }

    fun setEntryLiked(entry: com.thomasnorlund.archiveplayer.core.catalog.Entry, on: Boolean) = library.update { s ->
        val ref = entry.ref()
        val liked = s.liked
        val next = if (on) liked.entries.filterNot { it == ref } + ref else liked.entries.filterNot { it == ref }
        s.copy(playlists = s.playlists.map { if (it.isLiked) liked.copy(entries = next) else it })
    }

    fun setFollowing(identifier: String, on: Boolean) = library.update { s ->
        s.copy(follows = if (on) (s.follows - identifier) + identifier else s.follows - identifier)
    }

    fun createPlaylist(name: String, entries: List<com.thomasnorlund.archiveplayer.core.playlist.EntryRef>): String {
        val id = java.util.UUID.randomUUID().toString()
        library.update { s -> s.copy(playlists = s.playlists + com.thomasnorlund.archiveplayer.core.playlist.Playlist(id, name, null, entries)) }
        return id
    }

    fun addToPlaylist(playlistId: String, entries: List<com.thomasnorlund.archiveplayer.core.playlist.EntryRef>) = library.update { s ->
        s.copy(playlists = s.playlists.map { if (it.id == playlistId) com.thomasnorlund.archiveplayer.core.playlist.PlaylistRules.addItemEntries(it, entries) else it })
    }

    var libraryFilter by mutableStateOf("playlists")
    var deletedPlaylist by mutableStateOf<Pair<Int, com.thomasnorlund.archiveplayer.core.playlist.Playlist>?>(null)
        private set

    private fun updatePlaylist(id: String, change: (com.thomasnorlund.archiveplayer.core.playlist.Playlist) -> com.thomasnorlund.archiveplayer.core.playlist.Playlist) =
        library.update { s -> s.copy(playlists = s.playlists.map { if (it.id == id) change(it) else it }) }

    fun renamePlaylist(id: String, title: String) = updatePlaylist(id) { com.thomasnorlund.archiveplayer.core.playlist.PlaylistRules.rename(it, title) }
    fun describePlaylist(id: String, description: String) = updatePlaylist(id) { it.copy(description = description.trim().ifEmpty { null }) }
    fun reorderPlaylist(id: String, from: Int, to: Int) = updatePlaylist(id) { com.thomasnorlund.archiveplayer.core.playlist.PlaylistRules.reorder(it, from, to) }
    fun removeFromPlaylist(id: String, index: Int) = updatePlaylist(id) { com.thomasnorlund.archiveplayer.core.playlist.PlaylistRules.remove(it, index) }

    fun deletePlaylist(id: String) {
        val all = library.state.playlists
        val index = all.indexOfFirst { it.id == id }
        val playlist = all.getOrNull(index)?.takeIf { com.thomasnorlund.archiveplayer.core.playlist.PlaylistRules.canDelete(it) } ?: return
        deletedPlaylist = index to playlist
        library.update { s -> s.copy(playlists = s.playlists.filterNot { it.id == id }) }
    }

    fun undoDelete() {
        val (index, playlist) = deletedPlaylist ?: return
        deletedPlaylist = null
        library.update { s -> s.copy(playlists = s.playlists.toMutableList().apply { add(index.coerceAtMost(size), playlist) }) }
    }

    fun dismissUndo() { deletedPlaylist = null }

    fun clearHistory() = library.update { it.copy(history = emptyList()) }

    fun downloadPlaylist(playlist: com.thomasnorlund.archiveplayer.core.playlist.Playlist) {
        viewModelScope.launch {
            val resolved = playlist.entries.mapNotNull { ref ->
                val item = runCatching { repo.metadata(ref.identifier) }.getOrNull() ?: return@mapNotNull null
                item.entries.firstOrNull { e -> e.formats.any { it.fileName == ref.fileName } }?.let { item to it }
            }
            playlistVideoNotice = resolved.count { it.second.kind == MediaKind.VIDEO }
            downloads.enqueue(resolved.mapNotNull { (item, e) -> downloadRecord(item, e) })
        }
    }
    data class ImportPreview(
        val title: String,
        val description: String?,
        val entries: List<com.thomasnorlund.archiveplayer.core.playlist.EntryRef>,
        val skipped: Int = 0,
        val invalid: Boolean = false,
    )

    var pendingImport by mutableStateOf<ImportPreview?>(null)
    var shareTooLong by mutableStateOf<com.thomasnorlund.archiveplayer.core.playlist.Playlist?>(null)

    private fun startChooser(context: android.content.Context, send: android.content.Intent) {
        val chooser = android.content.Intent.createChooser(send, null)
        if (context !is android.app.Activity) chooser.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }

    fun sharePlaylist(context: android.content.Context, playlist: com.thomasnorlund.archiveplayer.core.playlist.Playlist) {
        val limit = AppEnvironment.shareLinkMaxLength
        val shared = com.thomasnorlund.archiveplayer.core.playlist.ShareLinkCodec.share(playlist).let { r ->
            if (limit != null && r is com.thomasnorlund.archiveplayer.core.playlist.ShareResult.Link && r.url.length > limit)
                com.thomasnorlund.archiveplayer.core.playlist.ShareResult.FileRequired(r.url.length) else r
        }
        when (val r = shared) {
            is com.thomasnorlund.archiveplayer.core.playlist.ShareResult.Link -> startChooser(context,
                android.content.Intent(android.content.Intent.ACTION_SEND).setType("text/plain")
                    .putExtra(android.content.Intent.EXTRA_SUBJECT, playlist.title)
                    .putExtra(android.content.Intent.EXTRA_TEXT, r.url))
            is com.thomasnorlund.archiveplayer.core.playlist.ShareResult.FileRequired -> shareTooLong = playlist
        }
    }

    fun sharePlaylistFile(context: android.content.Context, playlist: com.thomasnorlund.archiveplayer.core.playlist.Playlist) {
        shareTooLong = null
        val dir = java.io.File(context.cacheDir, "shared").apply { mkdirs() }
        val name = playlist.title.replace(Regex("[^\\p{L}\\p{N} _-]"), "_").trim().ifBlank { "playlist" }
        val file = java.io.File(dir, "$name.archiveplaylist")
        file.writeText(com.thomasnorlund.archiveplayer.core.playlist.PlaylistFileCodec.encode(playlist))
        val uri = androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.files", file)
        startChooser(context, android.content.Intent(android.content.Intent.ACTION_SEND).setType("application/json")
            .putExtra(android.content.Intent.EXTRA_STREAM, uri)
            .addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION))
    }

    /** Share links (App Link) and `.archiveplaylist` files open the import preview (R55, R56). */
    @Suppress("DEPRECATION")
    fun handleIntent(context: android.content.Context, intent: android.content.Intent?) {
        intent ?: return
        val uri: android.net.Uri? = when (intent.action) {
            android.content.Intent.ACTION_VIEW -> intent.data
            android.content.Intent.ACTION_SEND -> intent.getParcelableExtra<android.net.Uri>(android.content.Intent.EXTRA_STREAM)
                ?: intent.getStringExtra(android.content.Intent.EXTRA_TEXT)?.let { Regex("https://\\S+").find(it)?.value }?.let(android.net.Uri::parse)
            else -> null
        }
        uri ?: return
        val preview = if (uri.scheme == "https" || uri.scheme == "http") {
            com.thomasnorlund.archiveplayer.core.playlist.ShareLinkCodec.decode(uri.toString())?.let { ImportPreview(it.title, it.description, it.entries) }
        } else {
            runCatching { context.contentResolver.openInputStream(uri)!!.use { it.readBytes().toString(Charsets.UTF_8) } }.getOrNull()?.let { json ->
                val report = com.thomasnorlund.archiveplayer.core.playlist.PlaylistFileCodec.decode(json, "import")
                report.playlist?.let { ImportPreview(it.title, it.description, it.entries, report.skipped.size) }
            }
        }
        pendingImport = preview ?: ImportPreview("", null, emptyList(), invalid = true)
        stacks.getValue(tab).removeAll { it == Screen.Import }
        push(Screen.Import)
    }

    private fun uniquePlaylistName(title: String): String {
        val names = library.state.playlists.map { it.title }.toSet()
        if (title !in names) return title
        var n = 2
        while ("$title ($n)" in names) n++
        return "$title ($n)"
    }

    fun confirmImport() {
        val p = pendingImport ?: return
        if (!p.invalid) {
            val id = createPlaylist(uniquePlaylistName(p.title), p.entries)
            if (p.description != null) library.update { s -> s.copy(playlists = s.playlists.map { if (it.id == id) it.copy(description = p.description) else it }) }
        }
        cancelImport()
    }

    fun cancelImport() {
        pendingImport = null
        Tab.entries.forEach { t -> stacks.getValue(t).removeAll { it == Screen.Import } }
    }

    val player = com.thomasnorlund.archiveplayer.playback.PlayerConnection(application)
    var playerOpen by mutableStateOf(false)
    var sleepOption by mutableStateOf("off"); private set
    var sleepEndsAt by mutableStateOf<Long?>(null); private set
    var chosenSubtitle by mutableStateOf<Int?>(null); private set
    private val queueInfo = java.util.concurrent.ConcurrentHashMap<String, Pair<ArchiveItem, Entry>>()
    private val playingFormat = java.util.concurrent.ConcurrentHashMap<String, MediaFormat>()
    private val triedFormats = java.util.concurrent.ConcurrentHashMap<String, MutableSet<MediaFormat>>()
    private val qualityChoice = androidx.compose.runtime.mutableStateMapOf<String, String>()

    data class QualityOption(val key: String, val label: String, val auto: Boolean)

    private fun metered(): Boolean = AppEnvironment.networkOverride?.let { it == NetworkKind.METERED }
        ?: (getApplication<Application>().getSystemService(android.net.ConnectivityManager::class.java)?.isActiveNetworkMetered == true)

    private fun conditions() = PlaybackConditions(metered = metered(), dataSaver = settings?.dataSaver == true, forCast = false)

    private fun formatKey(f: MediaFormat) = f.format.trim().lowercase().replace(Regex("[^a-z0-9]+"), "_").trim('_')

    private fun mediaItem(item: ArchiveItem, e: Entry, format: MediaFormat? = null): MediaItem? {
        val f = format ?: FormatSelector.select(e, conditions()) ?: return null
        val ref = e.ref()
        if (format == null) triedFormats.remove("${item.identifier}/${ref.fileName}")
        val subtitles = e.subtitles.map { st ->
            MediaItem.SubtitleConfiguration.Builder(android.net.Uri.parse(repo.downloadUrl(item.identifier, st.fileName)))
                .setMimeType(if (st.type == SubtitleType.VTT) MimeTypes.TEXT_VTT else MimeTypes.APPLICATION_SUBRIP)
                .setLanguage(st.language)
                .build()
        }
        val entry = QueueEntry(item.identifier, ref.fileName, e.title, item.title, item.creator,
            downloads.playableFile(item.identifier, ref.fileName)?.takeIf { format == null }?.let { android.net.Uri.fromFile(it).toString() }
                ?: repo.downloadUrl(item.identifier, f.fileName), repo.thumbnailUrl(item.identifier), e.kind == MediaKind.VIDEO, subtitles)
        val id = "${item.identifier}/${ref.fileName}"
        queueInfo[id] = item to e
        playingFormat[id] = f
        return entry.toMediaItem()
    }

    private fun random() = AppEnvironment.shuffleSeed?.let { kotlin.random.Random(it) } ?: kotlin.random.Random.Default

    private fun startQueue(pairs: List<Pair<Entry, MediaItem>>, startEntry: Entry?, shuffle: Boolean) {
        if (pairs.isEmpty()) return
        val list = if (shuffle) pairs.shuffled(random()) else pairs
        val start = if (shuffle) 0 else list.indexOfFirst { it.first == startEntry }.coerceAtLeast(0)
        player.setQueue(list.map { it.second }, start)
    }

    fun playEntries(item: ArchiveItem, entries: List<Entry>, start: Int, shuffle: Boolean = false) =
        startQueue(entries.mapNotNull { e -> mediaItem(item, e)?.let { e to it } }, entries.getOrNull(start), shuffle)

    fun enqueue(item: ArchiveItem, entry: Entry, next: Boolean) {
        mediaItem(item, entry)?.let { player.add(listOf(it), next) }
    }

    fun playRefs(refs: List<com.thomasnorlund.archiveplayer.core.playlist.EntryRef>, start: Int, shuffle: Boolean = false) {
        viewModelScope.launch {
            val pairs = refs.mapNotNull { ref ->
                val item = runCatching { repo.metadata(ref.identifier) }.getOrNull() ?: return@mapNotNull null
                val e = item.entries.firstOrNull { e -> e.formats.any { it.fileName == ref.fileName } } ?: return@mapNotNull null
                mediaItem(item, e)?.let { e to it }
            }
            val startRef = refs.getOrNull(start)
            startQueue(pairs, pairs.firstOrNull { it.first.formats.any { f -> f.fileName == startRef?.fileName } }?.first, shuffle)
        }
    }

    fun setSleep(option: String) {
        sleepOption = option
        sleepEndsAt = option.toIntOrNull()?.let { System.currentTimeMillis() + it * 60_000L }
        player.setPauseAtEnd(option == "end")
    }

    private fun current(): Pair<ArchiveItem, Entry>? = player.currentId?.let { queueInfo[it] }

    fun isCurrentLiked(): Boolean = player.currentId?.let { id -> library.state.liked.entries.any { "${it.identifier}/${it.fileName}" == id } } == true

    fun toggleCurrentLike() { current()?.let { (_, e) -> setEntryLiked(e, !isCurrentLiked()) } }

    fun currentSubtitles(): List<com.thomasnorlund.archiveplayer.core.catalog.SubtitleTrack> = current()?.second?.subtitles.orEmpty()

    fun chooseSubtitle(index: Int?) {
        chosenSubtitle = index
        val track = index?.let { currentSubtitles().getOrNull(it) }
        player.withPlayer { c ->
            c.trackSelectionParameters = c.trackSelectionParameters.buildUpon()
                .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, track == null)
                .setPreferredTextLanguage(track?.language)
                .build()
        }
    }

    fun qualityOptions(): List<QualityOption> {
        val (_, e) = current() ?: return emptyList()
        val auto = FormatSelector.select(e, conditions())
        return FormatSelector.alternatives(e, forCast = false).map { QualityOption(formatKey(it), it.format, it == auto) }
    }

    fun chosenQuality(): String? = player.currentId?.let { qualityChoice[it] }

    /** Applies to the current entry only (UI_PLAYER_SPEC.md P09). */
    fun chooseQuality(key: String?) {
        val id = player.currentId ?: return
        val (item, e) = queueInfo[id] ?: return
        if (key == null) qualityChoice.remove(id) else qualityChoice[id] = key
        val format = key?.let { k -> e.formats.firstOrNull { formatKey(it) == k } }
        mediaItem(item, e, format)?.let { player.replaceCurrent(it, player.positionMs) }
    }

    /** Silent fallback to the next format; false when nothing is left (P14). */
    private fun fallback(): Boolean {
        val id = player.currentId ?: return false
        val (item, e) = queueInfo[id] ?: return false
        val failed = playingFormat[id] ?: return false
        val tried = triedFormats.getOrPut(id) { mutableSetOf() }.apply { add(failed) }
        var next = FormatSelector.fallback(e, failed, conditions())
        val seen = mutableSetOf<MediaFormat>()
        while (next != null && next in tried && seen.add(next)) next = FormatSelector.fallback(e, next, conditions())
        if (next != null && next in tried) next = null
        if (next == null) return false
        mediaItem(item, e, next)?.let { player.replaceCurrent(it, player.positionMs) } ?: return false
        return true
    }

    init {
        player.onError = { _, _ -> fallback() }
        player.onStarted = { id ->
            previousStartedId = currentStartedId
            currentStartedId = id
            queueInfo[id]?.let { (item, e) -> recordHistory(item, e) }
        }
        player.onItemEnded = { p -> markFinished(if (p.playbackState == androidx.media3.common.Player.STATE_ENDED) currentStartedId else previousStartedId) }
        viewModelScope.launch {
            var tick = 0
            var wasPlaying = false
            while (true) {
                delay(1_000)
                tick++
                val playing = player.isPlaying
                if (player.hasQueue && ((wasPlaying && !playing) || (playing && tick % 5 == 0))) savePosition()
                wasPlaying = playing
                val end = sleepEndsAt
                if (end != null && System.currentTimeMillis() >= end) { player.pause(); sleepOption = "off"; sleepEndsAt = null }
                if (sleepOption == "end" && !player.isPlaying && player.durationMs > 0 && player.positionMs >= player.durationMs - 1_500) setSleep("off")
            }
        }
    }

    private fun recordHistory(item: ArchiveItem, e: Entry) {
        if (settings?.historyPaused == true) return
        val ref = e.ref()
        // Keep the last saved position: a reconnecting controller reports the current entry as started again.
        val previous = library.state.history.firstOrNull { it.ref == ref }
        val event = com.thomasnorlund.archiveplayer.core.home.HistoryEvent(ref, item.identifier, item.title, item.creator, item.collections, item.subjects,
            System.currentTimeMillis(), previous?.positionMs ?: 0L, previous?.durationMs?.takeIf { it > 0 } ?: ((e.durationSeconds ?: 0.0) * 1000).toLong())
        library.update { s -> s.copy(history = (listOf(event) + s.history.filterNot { it.ref == ref }).take(500)) }
    }

    private var currentStartedId: String? = null
    private var previousStartedId: String? = null

    private fun updateHistory(id: String?, change: (com.thomasnorlund.archiveplayer.core.home.HistoryEvent) -> com.thomasnorlund.archiveplayer.core.home.HistoryEvent) {
        if (settings?.historyPaused == true) return
        val (_, e) = id?.let { queueInfo[it] } ?: return
        val ref = e.ref()
        library.update { s -> s.copy(history = s.history.map { if (it.ref == ref) change(it) else it }) }
    }

    private fun savePosition() {
        val pos = player.positionMs
        val dur = player.durationMs
        android.util.Log.i("ArchivePlayer", "savePosition ${player.currentId} pos=$pos dur=$dur")
        updateHistory(player.currentId) { it.copy(positionMs = pos, durationMs = if (dur > 0) dur else it.durationMs) }
    }

    private fun markFinished(id: String?) = updateHistory(id) { it.copy(positionMs = it.durationMs) }

    /** Continue card: queues the item and starts the entry at its stored position (A17). */
    fun resume(event: com.thomasnorlund.archiveplayer.core.home.HistoryEvent) {
        viewModelScope.launch {
            val item = runCatching { repo.metadata(event.itemIdentifier) }.getOrNull() ?: return@launch
            val pairs = item.entries.mapNotNull { e -> mediaItem(item, e)?.let { e to it } }
            val start = pairs.indexOfFirst { it.first.ref() == event.ref }.coerceAtLeast(0)
            if (pairs.isNotEmpty()) player.setQueue(pairs.map { it.second }, start, event.positionMs)
        }
    }

    val homeListState = androidx.compose.foundation.lazy.LazyListState()
    val homeRowHits = androidx.compose.runtime.mutableStateMapOf<String, List<com.thomasnorlund.archiveplayer.core.catalog.SearchHit>>()
    val homeRowStatus = androidx.compose.runtime.mutableStateMapOf<String, String>()
    var newInFollowed by mutableStateOf<List<com.thomasnorlund.archiveplayer.core.catalog.SearchHit>>(emptyList()); private set
    var homeRefreshing by mutableStateOf(false); private set
    private var homeLoaded = false

    fun loadHome(force: Boolean = false) {
        android.util.Log.i("ArchivePlayer", "loadHome force=$force loaded=$homeLoaded")
        if (homeLoaded && !force) return
        homeLoaded = true
        repo.categoryMap.roots.forEach { loadHomeRow(it.id) }
        if (force) refreshFollowed(manual = true)
    }

    private val homeRowJobs = mutableMapOf<String, kotlinx.coroutines.Job>()

    fun loadHomeRow(categoryId: String) {
        val c = repo.categoryMap.find(categoryId) ?: return
        val row = "category_$categoryId"
        homeRowStatus[row] = "loading"
        homeRowJobs[row]?.cancel()
        homeRowJobs[row] = viewModelScope.launch {
            val slow = launch { delay(15_000); if (homeRowStatus[row] == "loading") homeRowStatus[row] = "slow" }
            val kind = when (c.kind) {
                MediaKind.AUDIO -> com.thomasnorlund.archiveplayer.core.search.KindFilter.AUDIO
                MediaKind.VIDEO -> com.thomasnorlund.archiveplayer.core.search.KindFilter.VIDEO
                null -> com.thomasnorlund.archiveplayer.core.search.KindFilter.ALL
            }
            val request = com.thomasnorlund.archiveplayer.core.search.SearchRequest("", kind = kind, category = c.query,
                sort = com.thomasnorlund.archiveplayer.core.search.SortOrder.POPULARITY, rows = 30)
            val result = runCatching { repo.search(request, settings?.showAdultContent == true).hits }
            slow.cancel()
            result.onSuccess { homeRowHits[row] = it; homeRowStatus[row] = "ok" }
                .onFailure {
                if (it is kotlinx.coroutines.CancellationException) throw it
                android.util.Log.w("ArchivePlayer", "home row $row failed: ${it.message}")
                homeRowStatus[row] = "error"
            }
        }
    }

    /** New items in followed collections, at most once per throttle window unless pulled to refresh (H06). */
    fun refreshFollowed(manual: Boolean) {
        val s = library.state
        if (s.follows.isEmpty()) return
        val now = System.currentTimeMillis()
        if (!com.thomasnorlund.archiveplayer.core.home.FollowRefreshPolicy.shouldRefresh(s.followCheckMs, now, manual)) return
        if (manual) homeRefreshing = true
        viewModelScope.launch {
            val hits = s.follows.flatMap { id ->
                val request = com.thomasnorlund.archiveplayer.core.search.SearchRequest("",
                    category = com.thomasnorlund.archiveplayer.core.category.CategoryQuery(listOf(id), emptyList(), null),
                    sort = com.thomasnorlund.archiveplayer.core.search.SortOrder.NEWEST, rows = 20)
                runCatching { repo.search(request, settings?.showAdultContent == true).hits }.getOrDefault(emptyList())
            }
            newInFollowed = hits.distinctBy { it.identifier }
            library.update { it.copy(followCheckMs = now) }
            homeRefreshing = false
        }
    }
}
