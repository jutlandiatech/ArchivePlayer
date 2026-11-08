package com.thomasnorlund.archiveplayer.core.category

import com.thomasnorlund.archiveplayer.core.catalog.MediaKind
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.intOrNull

data class CategoryQuery(val collections: List<String>, val subjects: List<String>, val extra: String?)

data class Category(
    val id: String,
    val names: Map<String, String>,
    val kind: MediaKind?,
    val children: List<Category>,
    val query: CategoryQuery?,
) {
    /** Localized name with English fallback (R68). */
    fun name(isoCode: String): String = names[isoCode] ?: names["en"] ?: id
}

data class CategoryMap(val schemaVersion: Int, val version: String, val roots: List<Category>) {
    private val index by lazy { all().associateBy { it.id } }
    fun find(id: String): Category? = index[id]
    fun all(): List<Category> {
        val out = mutableListOf<Category>()
        fun walk(c: Category) { out += c; c.children.forEach(::walk) }
        roots.forEach(::walk)
        return out
    }
    fun parentOf(id: String): Category? = all().firstOrNull { c -> c.children.any { it.id == id } }
}

class InvalidCategoryMapException(message: String) : RuntimeException(message)

object CategoryMapParser {
    const val SUPPORTED_SCHEMA = 1

    private fun fail(message: String): Nothing = throw InvalidCategoryMapException(message)

    private fun strings(o: JsonObject, key: String): List<String> = when (val v = o[key]) {
        null -> emptyList()
        is JsonArray -> v.map { (it as? JsonPrimitive)?.takeIf { p -> p.isString }?.content ?: fail("$key must hold strings") }
        else -> fail("$key must be an array")
    }

    private fun category(el: Any?, ids: MutableSet<String>): Category {
        val o = el as? JsonObject ?: fail("category must be an object")
        val id = (o["id"] as? JsonPrimitive)?.content?.takeIf { it.isNotBlank() } ?: fail("category without id")
        if (!ids.add(id)) fail("duplicate id $id")
        val names = (o["names"] as? JsonObject)?.mapValues { (_, v) ->
            (v as? JsonPrimitive)?.takeIf { it.isString }?.content ?: fail("$id: names must be strings")
        } ?: fail("$id: missing names")
        if (names["en"].isNullOrBlank()) fail("$id: missing English name")
        val kind = when ((o["kind"] as? JsonPrimitive)?.content) {
            null -> null
            "video" -> MediaKind.VIDEO
            "audio" -> MediaKind.AUDIO
            else -> fail("$id: unknown kind")
        }
        val children = (o["children"] as? JsonArray)?.map { category(it, ids) } ?: emptyList()
        val query = (o["query"] as? JsonObject)?.let { q ->
            CategoryQuery(strings(q, "collections"), strings(q, "subjects"), (q["extra"] as? JsonPrimitive)?.content)
        }
        if (children.isEmpty() && (query == null || (query.collections.isEmpty() && query.subjects.isEmpty() && query.extra.isNullOrBlank()))) {
            fail("$id: leaf without query")
        }
        return Category(id, names, kind, children, query)
    }

    /** Throws [InvalidCategoryMapException] for invalid JSON, wrong schema, duplicate IDs or a category without English name. */
    fun parse(json: String): CategoryMap {
        val root = try {
            Json.parseToJsonElement(json) as? JsonObject
        } catch (e: Exception) {
            null
        } ?: fail("not a JSON object")
        val schema = (root["schemaVersion"] as? JsonPrimitive)?.intOrNull
        if (schema != SUPPORTED_SCHEMA) fail("unsupported schemaVersion $schema")
        val version = (root["version"] as? JsonPrimitive)?.content ?: fail("missing version")
        val ids = mutableSetOf<String>()
        val roots = (root["categories"] as? JsonArray)?.map { category(it, ids) } ?: fail("missing categories")
        return CategoryMap(schema, version, roots)
    }
}

enum class MapSource { REMOTE, CACHED, BUNDLED }

data class ChosenMap(val map: CategoryMap, val source: MapSource)

/** Remote → cached → bundled fallback; never a partially parsed map (R15, A08, A09). */
object CategoryMapSelector {
    private fun tryParse(json: String?) = json?.let { runCatching { CategoryMapParser.parse(it) }.getOrNull() }

    fun choose(remote: String?, cached: String?, bundled: String): ChosenMap =
        tryParse(remote)?.let { ChosenMap(it, MapSource.REMOTE) }
            ?: tryParse(cached)?.let { ChosenMap(it, MapSource.CACHED) }
            ?: ChosenMap(CategoryMapParser.parse(bundled), MapSource.BUNDLED)
}
