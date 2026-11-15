package com.jutlandiatech.archiveplayer.data

import android.content.Context
import android.net.ConnectivityManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.jutlandiatech.archiveplayer.AppEnvironment
import com.jutlandiatech.archiveplayer.NetworkKind
import com.jutlandiatech.archiveplayer.core.download.Availability
import com.jutlandiatech.archiveplayer.core.download.DownloadFileOps
import com.jutlandiatech.archiveplayer.core.download.DownloadMover
import com.jutlandiatech.archiveplayer.core.download.DownloadPolicy
import com.jutlandiatech.archiveplayer.core.download.StoragePolicy
import com.jutlandiatech.archiveplayer.core.download.StoredDownload
import com.jutlandiatech.archiveplayer.core.download.Volume
import com.jutlandiatech.archiveplayer.core.playlist.EntryRef
import com.jutlandiatech.archiveplayer.core.download.NetworkState
import com.jutlandiatech.archiveplayer.core.settings.DownloadLocation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

enum class DownloadState { QUEUED, WAITING_WIFI, DOWNLOADING, DONE, FAILED }

/** One downloaded (or pending) audio entry: [fileName] identifies the entry, [formatFile] is the file actually stored. */
data class DownloadRecord(
    val identifier: String,
    val fileName: String,
    val formatFile: String,
    val title: String,
    val itemTitle: String,
    val url: String,
    val location: DownloadLocation,
    val state: DownloadState = DownloadState.QUEUED,
    val sizeBytes: Long = 0,
    val doneBytes: Long = 0,
    val mobileAllowed: Boolean = false,
) {
    val key get() = "$identifier/$fileName"
}

/** Audio downloads (R59–R61): persisted list, Wi-Fi-only gate with a per-download mobile-data override, sequential worker. */
class Downloads(context: Context, private val wifiOnly: () -> Boolean) {
    private val app = context.applicationContext
    private val file = File(app.filesDir, "downloads.json")
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var worker: Job? = null

    var records: List<DownloadRecord> by mutableStateOf(load())
        private set

    init {
        records = records.map { if (it.state == DownloadState.DOWNLOADING) it.copy(state = DownloadState.QUEUED) else it }
        pump()
    }

    fun dir(location: DownloadLocation): File? = when (location) {
        DownloadLocation.INTERNAL -> File(app.filesDir, "downloads")
        DownloadLocation.SD_CARD -> sdRoot()?.let { File(it, "downloads") }
    }

    /** App folder on the removable volume (R85, no storage permission), or null while no card is mounted. */
    private fun sdRoot(): File? =
        if (AppEnvironment.sdCardOverride) AppEnvironment.sdCardDir
        else app.getExternalFilesDirs(null).drop(1).firstOrNull { it != null && android.os.Environment.getExternalStorageState(it) == android.os.Environment.MEDIA_MOUNTED }

    fun sdCardMounted(): Boolean = sdRoot() != null

    fun volumes(): List<Volume> = DownloadLocation.entries.map { loc ->
        Volume(loc, loc == DownloadLocation.INTERNAL || sdCardMounted(), freeBytes(loc), usedBytes(loc))
    }

    fun stored(): List<StoredDownload> = records.filter { it.state == DownloadState.DONE }
        .map { StoredDownload(EntryRef(it.identifier, it.fileName), it.location, it.sizeBytes) }

    fun availability(r: DownloadRecord): Availability =
        StoragePolicy.availability(StoredDownload(EntryRef(r.identifier, r.fileName), r.location, r.sizeBytes), volumes())

    var moving: Boolean by mutableStateOf(false)
        private set

    /** Moves finished downloads to [target]; each source is deleted only after its copy is verified (R85). */
    fun moveTo(target: DownloadLocation) {
        if (moving || dir(target) == null) return
        moving = true
        scope.launch {
            try {
                val byRef = records.associateBy { EntryRef(it.identifier, it.fileName) }
                val ops = object : DownloadFileOps {
                    private fun src(s: StoredDownload) = byRef[s.ref]?.let(::localFile)
                    private fun dst(s: StoredDownload) = byRef[s.ref]?.let { localFile(it.copy(location = target)) }
                    override fun copy(download: StoredDownload, target: DownloadLocation): Boolean = runCatching {
                        val from = src(download) ?: return false
                        val to = dst(download) ?: return false
                        if (!from.isFile) return to.isFile
                        to.parentFile?.mkdirs()
                        from.copyTo(to, overwrite = true)
                        true
                    }.getOrDefault(false)
                    override fun verifyCopy(download: StoredDownload, target: DownloadLocation): Boolean {
                        val to = dst(download) ?: return false
                        val from = src(download)
                        return to.isFile && (from == null || !from.isFile || from.length() == to.length())
                    }
                    override fun delete(download: StoredDownload) {
                        src(download)?.let { f -> f.delete(); f.parentFile?.takeIf { it.list().isNullOrEmpty() }?.delete() }
                    }
                }
                val outcome = DownloadMover(ops).move(stored(), target)
                val moved = outcome.moved.map { it.ref }.toSet()
                update { l -> l.map { if (EntryRef(it.identifier, it.fileName) in moved) it.copy(location = target) else it } }
            } finally {
                moving = false
            }
        }
    }

    fun localFile(r: DownloadRecord): File? = dir(r.location)?.let { File(File(it, r.identifier), r.formatFile.replace('/', '_')) }

    /** Local file for a finished download whose volume is present, else null (stream instead). */
    fun playableFile(identifier: String, fileName: String): File? =
        records.firstOrNull { it.identifier == identifier && it.fileName == fileName && it.state == DownloadState.DONE }
            ?.let(::localFile)?.takeIf { it.isFile }

    fun freeBytes(location: DownloadLocation): Long =
        AppEnvironment.freeBytesOverride ?: (dir(location)?.let { d -> d.mkdirs(); d.usableSpace } ?: 0L)

    fun usedBytes(location: DownloadLocation? = null): Long =
        records.filter { it.state == DownloadState.DONE && (location == null || it.location == location) }.sumOf { it.sizeBytes }

    fun network(): NetworkState = when (AppEnvironment.networkOverride) {
        NetworkKind.WIFI -> NetworkState(connected = true, unmetered = true)
        NetworkKind.METERED -> NetworkState(connected = true, unmetered = false)
        NetworkKind.OFFLINE -> NetworkState(connected = false, unmetered = false)
        null -> app.getSystemService(ConnectivityManager::class.java).let { cm ->
            NetworkState(connected = cm?.activeNetwork != null, unmetered = cm?.isActiveNetworkMetered == false)
        }
    }

    fun enqueue(list: List<DownloadRecord>) {
        update { current ->
            val keys = current.map { it.key }.toSet()
            current + list.filter { it.key !in keys }
        }
        pump()
    }

    fun allowMobile(identifier: String) {
        update { l -> l.map { if (it.identifier == identifier && it.state != DownloadState.DONE) it.copy(mobileAllowed = true, state = DownloadState.QUEUED) else it } }
        pump()
    }

    fun remove(predicate: (DownloadRecord) -> Boolean) {
        val gone = records.filter(predicate)
        update { l -> l.filterNot(predicate) }
        gone.forEach { r -> localFile(r)?.delete(); localFile(r)?.parentFile?.takeIf { it.list().isNullOrEmpty() }?.delete() }
    }

    fun removeAll() = remove { true }

    private fun pump() {
        if (worker?.isActive == true) return
        if (records.none { it.state == DownloadState.QUEUED || it.state == DownloadState.WAITING_WIFI }) return
        worker = scope.launch {
            while (isActive) {
                val net = network()
                val pending = records.filter { it.state == DownloadState.QUEUED || it.state == DownloadState.WAITING_WIFI }
                if (pending.isEmpty()) break
                val next = pending.firstOrNull { DownloadPolicy.canStart(wifiOnly(), net, it.mobileAllowed) }
                if (next == null) {
                    update { l -> l.map { if (it.state == DownloadState.QUEUED && net.connected) it.copy(state = DownloadState.WAITING_WIFI) else it } }
                    delay(3_000)
                    continue
                }
                fetch(next)
            }
        }.also { job ->
            DownloadKeepAlive.start(app)
            job.invokeOnCompletion { DownloadKeepAlive.active = false }
        }
    }

    private fun fetch(r: DownloadRecord) {
        set(r.key) { it.copy(state = DownloadState.DOWNLOADING) }
        val target = localFile(r)
        val ok = target != null && runCatching {
            target.parentFile?.mkdirs()
            val part = File(target.path + ".part")
            val conn = URL(r.url).openConnection() as HttpURLConnection
            conn.connectTimeout = 15_000
            conn.readTimeout = 30_000
            val have = if (part.exists()) part.length() else 0L
            if (have > 0) conn.setRequestProperty("Range", "bytes=$have-")
            try {
                val code = conn.responseCode
                if (code !in 200..299) error("HTTP $code")
                // Resume after a network drop when the server honours the byte range (R60).
                val resumed = code == 206 && have > 0
                val total = conn.contentLengthLong.coerceAtLeast(0) + if (resumed) have else 0L
                var done = if (resumed) have else 0L
                var lastReport = 0L
                conn.inputStream.use { input ->
                    java.io.FileOutputStream(part, resumed).use { out ->
                        val buf = ByteArray(64 * 1024)
                        while (true) {
                            val n = input.read(buf)
                            if (n < 0) break
                            out.write(buf, 0, n)
                            done += n
                            if (done - lastReport > 256 * 1024) {
                                lastReport = done
                                set(r.key) { it.copy(sizeBytes = total, doneBytes = done) }
                            }
                        }
                    }
                }
                if (!part.renameTo(target)) error("rename failed")
                set(r.key) { it.copy(state = DownloadState.DONE, sizeBytes = target.length(), doneBytes = target.length()) }
            } finally {
                conn.disconnect()
            }
        }.isSuccess
        if (!ok) set(r.key) { it.copy(state = if (network().connected) DownloadState.FAILED else DownloadState.QUEUED) }
    }

    private fun set(key: String, transform: (DownloadRecord) -> DownloadRecord) =
        update { l -> l.map { if (it.key == key) transform(it) else it } }

    private fun update(transform: (List<DownloadRecord>) -> List<DownloadRecord>) {
        synchronized(this) {
            records = transform(records)
            runCatching { file.writeText(encode(records)) }
        }
    }

    private fun encode(list: List<DownloadRecord>): String = JSONArray().apply {
        list.forEach { r ->
            put(JSONObject().put("identifier", r.identifier).put("fileName", r.fileName).put("formatFile", r.formatFile)
                .put("title", r.title).put("itemTitle", r.itemTitle).put("url", r.url).put("location", r.location.name)
                .put("state", r.state.name).put("sizeBytes", r.sizeBytes).put("doneBytes", r.doneBytes).put("mobileAllowed", r.mobileAllowed))
        }
    }.toString()

    private fun load(): List<DownloadRecord> = runCatching {
        val a = JSONArray(file.readText())
        (0 until a.length()).map { i ->
            val o = a.getJSONObject(i)
            DownloadRecord(o.getString("identifier"), o.getString("fileName"), o.getString("formatFile"), o.optString("title"),
                o.optString("itemTitle"), o.getString("url"), DownloadLocation.valueOf(o.optString("location", "INTERNAL")),
                DownloadState.valueOf(o.optString("state", "QUEUED")), o.optLong("sizeBytes"), o.optLong("doneBytes"), o.optBoolean("mobileAllowed"))
        }
    }.getOrDefault(emptyList())
}
