package com.thomasnorlund.archiveplayer.core.text

/** One UI language: flat key → text map from `TextCatalog_<CODE>.json` (R65, R66). */
class TextCatalog(
    val fileCode: String,
    val isoCode: String,
    val selfName: String,
    val texts: Map<String, String>,
) {
    /** Resolves [key], replacing `{name}` placeholders; missing or extra arguments throw [PlaceholderException]. */
    fun text(key: String, args: Map<String, Any> = emptyMap()): String = TODO()

    companion object {
        const val FILE_PREFIX = "TextCatalog_"

        /** Parses a catalog file; throws [InvalidCatalogException] for invalid JSON, non-string values or missing `language.self`/`language.code`. */
        fun parse(fileCode: String, json: String): TextCatalog = TODO()

        /** `TextCatalog_DEN.json` → `DEN`; null for any other file name. */
        fun codeFromFileName(fileName: String): String? = TODO()
    }
}

class InvalidCatalogException(message: String) : RuntimeException(message)
class MissingTextException(message: String) : RuntimeException(message)
class PlaceholderException(message: String) : RuntimeException(message)

/** Key-parity check against the English reference (R66, A04). */
object CatalogParity {
    /** Human-readable problems: missing keys, extra keys, blank values, placeholder-set mismatches. Empty when consistent. */
    fun problems(reference: TextCatalog, other: TextCatalog): List<String> = TODO()
}
