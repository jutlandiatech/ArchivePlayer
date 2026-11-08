package com.jutlandiatech.archiveplayer

import android.app.Application
import com.jutlandiatech.archiveplayer.cast.FlavourCast

class ArchivePlayerApp : Application() {
    override fun onCreate() {
        super.onCreate()
        FlavourCast.initialize(this)
    }
}
