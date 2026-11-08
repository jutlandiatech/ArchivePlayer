package com.thomasnorlund.archiveplayer.data

import android.content.Context
import com.thomasnorlund.archiveplayer.AppEnvironment
import com.thomasnorlund.archiveplayer.BuildConfig
import com.thomasnorlund.archiveplayer.core.catalog.ArchiveItem
import com.thomasnorlund.archiveplayer.core.catalog.MetadataParser
import com.thomasnorlund.archiveplayer.core.catalog.SearchPage
import com.thomasnorlund.archiveplayer.core.catalog.SearchResponseParser
import com.thomasnorlund.archiveplayer.core.category.CategoryMap
import com.thomasnorlund.archiveplayer.core.category.CategoryMapParser
import com.thomasnorlund.archiveplayer.core.filter.AdultFilter
import com.thomasnorlund.archiveplayer.core.filter.KeywordListParser
import com.thomasnorlund.archiveplayer.core.net.ArchiveClient
import com.thomasnorlund.archiveplayer.core.net.HttpResponse
import com.thomasnorlund.archiveplayer.core.net.HttpTransport
import com.thomasnorlund.archiveplayer.core.net.NetworkPolicy
import com.thomasnorlund.archiveplayer.core.net.Sleeper
import com.thomasnorlund.archiveplayer.core.search.AdvancedSearchUrl
import com.thomasnorlund.archiveplayer.core.search.SearchRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.util.concurrent.ConcurrentHashMap

/** HttpURLConnection transport for [ArchiveClient]; no third-party HTTP stack needed. */
private object UrlConnectionTransport : HttpTransport {
    override fun get(url: String, headers: Map<String, String>): HttpResponse {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.connectTimeout = 15_000
        conn.readTimeout = 40_000
        headers.forEach { (k, v) -> conn.setRequestProperty(k, v) }
        return try {
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val body = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            HttpResponse(code, body, conn.headerFields.filterKeys { it != null }.mapValues { it.value.firstOrNull().orEmpty() })
        } finally {
            conn.disconnect()
        }
    }
}

/** archive.org access: search, metadata and media URLs, honouring [AppEnvironment] test overrides. */
class ArchiveRepository(context: Context) {
    private val assets = context.applicationContext.assets
    private val client = ArchiveClient(UrlConnectionTransport, Sleeper { Thread.sleep(it) }, BuildConfig.VERSION_NAME)
    private val metadataCache = ConcurrentHashMap<String, ArchiveItem>()

    val categoryMap: CategoryMap by lazy { CategoryMapParser.parse(asset("catalog/category-map.json")) }
    val adultFilter: AdultFilter by lazy { AdultFilter(KeywordListParser.parse(asset("catalog/adult-keywords.json"))) }

    val base: String
        get() = AppEnvironment.archiveBaseUrl.trimEnd('/').also { url ->
            if (url != AppEnvironment.ARCHIVE_BASE_URL) {
                runCatching { URI(url).host }.getOrNull()?.let { NetworkPolicy.testHosts = setOf(it) }
            }
        }

    private fun asset(path: String) = assets.open(path).bufferedReader().use { it.readText() }

    suspend fun search(request: SearchRequest, showAdult: Boolean): SearchPage = withContext(Dispatchers.IO) {
        val filter = if (showAdult) null else adultFilter
        SearchResponseParser.parse(client.get(AdvancedSearchUrl.build(request, filter, base)))
    }

    suspend fun metadata(identifier: String): ArchiveItem? {
        metadataCache[identifier]?.let { return it }
        return withContext(Dispatchers.IO) {
            MetadataParser.parse(client.get("$base/metadata/$identifier"))?.also { metadataCache[identifier] = it }
        }
    }

    fun downloadUrl(identifier: String, fileName: String) = "$base/download/$identifier/${fileName.split('/').joinToString("/") { android.net.Uri.encode(it) }}"
    fun thumbnailUrl(identifier: String) = "$base/services/img/$identifier"
}
