package com.thomasnorlund.archiveplayer

import android.app.Application
import com.thomasnorlund.archiveplayer.cast.FlavourCast

class ArchivePlayerApp : Application() {
    override fun onCreate() {
        super.onCreate()
        FlavourCast.initialize(this)
    }
}
