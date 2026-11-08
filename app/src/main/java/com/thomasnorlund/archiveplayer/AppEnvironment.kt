package com.thomasnorlund.archiveplayer

enum class NetworkKind { WIFI, METERED, OFFLINE }

/**
 * Test seams (TESTING.md → "UI test seams"). Production code reads these at use time, never caches them.
 * Instrumentation tests point the app at a local fixture server and fake the network and storage state.
 */
object AppEnvironment {
    const val ARCHIVE_BASE_URL = "https://archive.org"
    const val PAGES_BASE_URL = "https://jutlandiatech.github.io/ArchivePlayer"

    /** Base for archive.org API, download and image requests. */
    @Volatile var archiveBaseUrl: String = ARCHIVE_BASE_URL

    /** Base for the GitHub Pages site (remote category map, share links are always built with [PAGES_BASE_URL]). */
    @Volatile var pagesBaseUrl: String = PAGES_BASE_URL

    /** When set, replaces the connectivity-manager network type (Wi-Fi-only, data saver). */
    @Volatile var networkOverride: NetworkKind? = null

    /** When set, replaces the free bytes reported for the current download location. */
    @Volatile var freeBytesOverride: Long? = null

    /** When set, shuffle uses `Random(shuffleSeed)`. */
    @Volatile var shuffleSeed: Long? = null

    fun reset() {
        archiveBaseUrl = ARCHIVE_BASE_URL
        pagesBaseUrl = PAGES_BASE_URL
        networkOverride = null
        freeBytesOverride = null
        shuffleSeed = null
    }
}
