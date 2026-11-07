package com.thomasnorlund.archiveplayer.core.filter

import com.thomasnorlund.archiveplayer.core.catalog.ArchiveItem
import com.thomasnorlund.archiveplayer.core.catalog.SearchHit

data class KeywordList(val version: String, val keywords: List<String>)

class InvalidKeywordListException(message: String) : RuntimeException(message)

object KeywordListParser {
    const val SUPPORTED_SCHEMA = 1
    fun parse(json: String): KeywordList = TODO()

    /** Remote → cached → bundled fallback, same rules as the category map (R71). */
    fun choose(remote: String?, cached: String?, bundled: String): KeywordList = TODO()
}

/** Best-effort adult-content detection (R70, R71). */
class AdultFilter(private val keywords: KeywordList) {
    fun isAdult(collections: List<String>, subjects: List<String>, title: String): Boolean = TODO()
    fun isAdult(item: ArchiveItem): Boolean = TODO()
    fun isAdult(hit: SearchHit): Boolean = TODO()

    /** Clause appended to advancedsearch queries while adult content is hidden. */
    val queryExclusion: String get() = TODO()
}
