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
    fun downloadItem(item: com.thomasnorlund.archiveplayer.core.catalog.ArchiveItem) = Unit
    fun downloadEntry(item: com.thomasnorlund.archiveplayer.core.catalog.ArchiveItem, entry: com.thomasnorlund.archiveplayer.core.catalog.Entry) = Unit
    fun downloadLabelKey(item: com.thomasnorlund.archiveplayer.core.catalog.ArchiveItem): String = "item.download"

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

    fun downloadPlaylist(playlist: com.thomasnorlund.archiveplayer.core.playlist.Playlist) = Unit
    fun sharePlaylist(context: android.content.Context, playlist: com.thomasnorlund.archiveplayer.core.playlist.Playlist) = Unit

    val player = com.thomasnorlund.archiveplayer.playback.PlayerConnection(application)
    var playerOpen by mutableStateOf(false)
    var sleepOption by mutableStateOf("off"); private set
    var sleepEndsAt by mutableStateOf<Long?>(null); private set
    var chosenSubtitle by mutableStateOf<Int?>(null); private set
    private val queueInfo = java.util.concurrent.ConcurrentHashMap<String, Pair<ArchiveItem, Entry>>()
    private val playingFormat = java.util.concurrent.ConcurrentHashMap<String, MediaFormat>()
    private val qualityChoice = androidx.compose.runtime.mutableStateMapOf<String, String>()

    data class QualityOption(val key: String, val label: String, val auto: Boolean)

    private fun metered(): Boolean = AppEnvironment.networkOverride?.let { it == NetworkKind.METERED }
        ?: (getApplication<Application>().getSystemService(android.net.ConnectivityManager::class.java)?.isActiveNetworkMetered == true)

    private fun conditions() = PlaybackConditions(metered = metered(), dataSaver = settings?.dataSaver == true, forCast = false)

    private fun formatKey(f: MediaFormat) = f.format.trim().lowercase().replace(Regex("[^a-z0-9]+"), "_").trim('_')

    private fun mediaItem(item: ArchiveItem, e: Entry, format: MediaFormat? = null): MediaItem? {
        val f = format ?: FormatSelector.select(e, conditions()) ?: return null
        val ref = e.ref()
        val subtitles = e.subtitles.map { st ->
            MediaItem.SubtitleConfiguration.Builder(android.net.Uri.parse(repo.downloadUrl(item.identifier, st.fileName)))
                .setMimeType(if (st.type == SubtitleType.VTT) MimeTypes.TEXT_VTT else MimeTypes.APPLICATION_SUBRIP)
                .setLanguage(st.language)
                .build()
        }
        val entry = QueueEntry(item.identifier, ref.fileName, e.title, item.title, item.creator,
            repo.downloadUrl(item.identifier, f.fileName), repo.thumbnailUrl(item.identifier), e.kind == MediaKind.VIDEO, subtitles)
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
        val next = FormatSelector.fallback(e, failed, conditions()) ?: return false
        mediaItem(item, e, next)?.let { player.replaceCurrent(it, player.positionMs) } ?: return false
        return true
    }

    init {
        player.onItemEnded = { p -> if (sleepOption == "end") { p.pause(); sleepOption = "off" } }
        player.onError = { _, _ -> fallback() }
        viewModelScope.launch {
            while (true) {
                delay(1_000)
                val end = sleepEndsAt
                if (end != null && System.currentTimeMillis() >= end) { player.pause(); sleepOption = "off"; sleepEndsAt = null }
            }
        }
    }
}
