package com.thomasnorlund.archiveplayer

import android.app.Application
import androidx.annotation.OptIn
import androidx.media3.cast.Cast
import androidx.media3.common.util.UnstableApi

class ArchivePlayerApp : Application() {
    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()
        Cast.getSingletonInstance(this).initialize()
    }
}
