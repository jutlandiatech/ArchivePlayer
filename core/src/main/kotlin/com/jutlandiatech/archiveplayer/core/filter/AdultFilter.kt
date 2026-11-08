package com.jutlandiatech.archiveplayer.core.filter

import com.jutlandiatech.archiveplayer.core.catalog.ArchiveItem
import com.jutlandiatech.archiveplayer.core.catalog.SearchHit
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.intOrNull

data class KeywordList(val version: String, val keywords: List<String>)

class InvalidKeywordListException(message: String) : RuntimeException(message)

object KeywordListParser {
    const val SUPPORTED_SCHEMA = 1
    fun parse(json: String): KeywordList {
        val root = try {
            Json.parseToJsonElement(json) as? JsonObject
        } catch (e: Exception) {
            null
        } ?: throw InvalidKeywordListException("not a JSON object")
        val schema = (root["schemaVersion"] as? JsonPrimitive)?.intOrNull
        if (schema != SUPPORTED_SCHEMA) throw InvalidKeywordListException("unsupported schemaVersion $schema")
        val version = (root["version"] as? JsonPrimitive)?.takeIf { it.isString }?.content
            ?: throw InvalidKeywordListException("missing version")
        val keywords = (root["keywords"] as? JsonArray)?.map {
            (it as? JsonPrimitive)?.takeIf { p -> p.isString }?.content?.trim()?.takeIf { k -> k.isNotEmpty() }
                ?: throw InvalidKeywordListException("keywords must be non-empty strings")
        } ?: throw InvalidKeywordListException("missing keywords")
        return KeywordList(version, keywords)
    }

    private fun tryParse(json: String?): KeywordList? = json?.let { runCatching { parse(it) }.getOrNull() }

    /** Remote → cached → bundled fallback, same rules as the category map (R71). */
    fun choose(remote: String?, cached: String?, bundled: String): KeywordList =
        tryParse(remote) ?: tryParse(cached) ?: parse(bundled)
}

/** Best-effort adult-content detection (R70, R71). */
class AdultFilter(private val keywords: KeywordList) {
    private val patterns = keywords.keywords.map {
        Regex("""(?<![\p{L}\p{N}])${Regex.escape(it)}(?![\p{L}\p{N}])""", RegexOption.IGNORE_CASE)
    }

    private fun matches(text: String) = patterns.any { it.containsMatchIn(text) }

    fun isAdult(collections: List<String>, subjects: List<String>, title: String): Boolean =
        collections.any { it.equals(NO_PREVIEW, ignoreCase = true) } || matches(title) || subjects.any { matches(it) }

    fun isAdult(item: ArchiveItem): Boolean = isAdult(item.collections, item.subjects, item.title)
    fun isAdult(hit: SearchHit): Boolean = isAdult(hit.collections, hit.subjects, hit.title)

    /** Clause appended to advancedsearch queries while adult content is hidden. */
    val queryExclusion: String get() = "-collection:$NO_PREVIEW"

    companion object {
        const val NO_PREVIEW = "no-preview"
    }
}
