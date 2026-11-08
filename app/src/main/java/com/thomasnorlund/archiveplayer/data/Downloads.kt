package com.thomasnorlund.archiveplayer.data

import android.content.Context
import android.net.ConnectivityManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.thomasnorlund.archiveplayer.AppEnvironment
import com.thomasnorlund.archiveplayer.NetworkKind
import com.thomasnorlund.archiveplayer.core.download.DownloadPolicy
import com.thomasnorlund.archiveplayer.core.download.NetworkState
import com.thomasnorlund.archiveplayer.core.settings.DownloadLocation
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
        DownloadLocation.SD_CARD -> app.getExternalFilesDirs(null).getOrNull(1)?.let { File(it, "downloads") }
    }

    fun sdCardMounted(): Boolean = app.getExternalFilesDirs(null).getOrNull(1) != null

    fun localFile(r: DownloadRecord): File? = dir(r.location)?.let { File(File(it, r.identifier), r.formatFile.replace('/', '_')) }

    /** Local file for a finished download whose volume is present, else null (stream instead). */
    fun playableFile(identifier: String, fileName: String): File? =
        records.firstOrNull { it.identifier == identifier && it.fileName == fileName && it.state == DownloadState.DONE }
            ?.let(::localFile)?.takeIf { it.isFile }

    fun freeBytes(location: DownloadLocation): Long =
        AppEnvironment.freeBytesOverride ?: (dir(location)?.let { d -> d.mkdirs(); d.usableSpace } ?: 0L)

    fun usedBytes(): Long = records.filter { it.state == DownloadState.DONE }.sumOf { it.sizeBytes }

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
            try {
                if (conn.responseCode !in 200..299) error("HTTP ${conn.responseCode}")
                val total = conn.contentLengthLong.coerceAtLeast(0)
                var done = 0L
                var lastReport = 0L
                conn.inputStream.use { input ->
                    part.outputStream().use { out ->
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
        if (!ok) set(r.key) { it.copy(state = DownloadState.FAILED) }
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
