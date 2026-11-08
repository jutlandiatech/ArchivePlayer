package com.jutlandiatech.archiveplayer.core.support

import java.io.File

object Fixtures {
    fun archive(name: String): String =
        requireNotNull(Fixtures::class.java.getResource("/fixtures/archive/$name")) { "Missing fixture $name" }.readText()

    /** Files shipped in the Android app's assets (single source of truth for catalogs and maps). */
    fun appAsset(path: String): File = File("../app/src/main/assets/$path")
}
