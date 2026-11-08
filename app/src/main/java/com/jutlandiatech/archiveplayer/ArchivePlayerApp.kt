package com.jutlandiatech.archiveplayer

import android.app.Application
import com.jutlandiatech.archiveplayer.cast.FlavourCast

class ArchivePlayerApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // R76: HttpURLConnection honours archive.org's cache headers for search and metadata responses.
        runCatching { android.net.http.HttpResponseCache.install(java.io.File(cacheDir, "http"), 20L * 1024 * 1024) }
        FlavourCast.initialize(this)
    }
}
