package com.thomasnorlund.archiveplayer.core.category

import com.thomasnorlund.archiveplayer.core.catalog.MediaKind

data class CategoryQuery(val collections: List<String>, val subjects: List<String>, val extra: String?)

data class Category(
    val id: String,
    val names: Map<String, String>,
    val kind: MediaKind?,
    val children: List<Category>,
    val query: CategoryQuery?,
) {
    /** Localized name with English fallback (R68). */
    fun name(isoCode: String): String = TODO()
}

data class CategoryMap(val schemaVersion: Int, val version: String, val roots: List<Category>) {
    fun find(id: String): Category? = TODO()
    fun all(): List<Category> = TODO()
}

class InvalidCategoryMapException(message: String) : RuntimeException(message)

object CategoryMapParser {
    const val SUPPORTED_SCHEMA = 1

    /** Throws [InvalidCategoryMapException] for invalid JSON, wrong schema, duplicate IDs or a category without English name. */
    fun parse(json: String): CategoryMap = TODO()
}

enum class MapSource { REMOTE, CACHED, BUNDLED }

data class ChosenMap(val map: CategoryMap, val source: MapSource)

/** Remote → cached → bundled fallback; never a partially parsed map (R15, A08, A09). */
object CategoryMapSelector {
    fun choose(remote: String?, cached: String?, bundled: String): ChosenMap = TODO()
}
