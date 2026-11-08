package com.jutlandiatech.archiveplayer.core.text

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/** One UI language: flat key → text map from `TextCatalog_<CODE>.json` (R65, R66). */
class TextCatalog(
    val fileCode: String,
    val isoCode: String,
    val selfName: String,
    val texts: Map<String, String>,
) {
    /** Resolves [key], replacing `{name}` placeholders; missing or extra arguments throw [PlaceholderException]. */
    fun text(key: String, args: Map<String, Any> = emptyMap()): String {
        val template = texts[key] ?: throw MissingTextException("No text for '$key' in $fileCode")
        val names = placeholders(template)
        if (names != args.keys) {
            throw PlaceholderException("'$key' in $fileCode expects $names but got ${args.keys}")
        }
        return PLACEHOLDER.replace(template) { args.getValue(it.groupValues[1]).toString() }
    }

    fun has(key: String): Boolean = key in texts

    companion object {
        const val FILE_PREFIX = "TextCatalog_"
        private val PLACEHOLDER = Regex("""\{([A-Za-z0-9_]+)\}""")
        private val FILE_NAME = Regex("""^TextCatalog_([A-Za-z]+)\.json$""")

        internal fun placeholders(text: String): Set<String> =
            PLACEHOLDER.findAll(text).map { it.groupValues[1] }.toSet()

        /** Parses a catalog file; throws [InvalidCatalogException] for invalid JSON, non-string values or missing `language.self`/`language.code`. */
        fun parse(fileCode: String, json: String): TextCatalog {
            val root = try {
                Json.parseToJsonElement(json)
            } catch (e: Exception) {
                throw InvalidCatalogException("$fileCode: invalid JSON (${e.message})")
            }
            if (root !is JsonObject) throw InvalidCatalogException("$fileCode: root must be an object")
            val texts = root.mapValues { (key, value) ->
                if (value !is JsonPrimitive || !value.isString) {
                    throw InvalidCatalogException("$fileCode: value of '$key' is not a string")
                }
                value.content
            }
            val self = texts["language.self"] ?: throw InvalidCatalogException("$fileCode: missing language.self")
            val iso = texts["language.code"] ?: throw InvalidCatalogException("$fileCode: missing language.code")
            return TextCatalog(fileCode, iso, self, texts)
        }

        /** `TextCatalog_DEN.json` → `DEN`; null for any other file name. */
        fun codeFromFileName(fileName: String): String? = FILE_NAME.matchEntire(fileName)?.groupValues?.get(1)
    }
}

class InvalidCatalogException(message: String) : RuntimeException(message)
class MissingTextException(message: String) : RuntimeException(message)
class PlaceholderException(message: String) : RuntimeException(message)

/** Key-parity check against the English reference (R66, A04). */
object CatalogParity {
    /** Human-readable problems: missing keys, extra keys, blank values, placeholder-set mismatches. Empty when consistent. */
    fun problems(reference: TextCatalog, other: TextCatalog): List<String> {
        val problems = mutableListOf<String>()
        val name = other.fileCode
        (reference.texts.keys - other.texts.keys).sorted().forEach { problems += "$name: missing key '$it'" }
        (other.texts.keys - reference.texts.keys).sorted().forEach { problems += "$name: extra key '$it'" }
        other.texts.toSortedMap().forEach { (key, value) ->
            if (value.isBlank()) problems += "$name: blank value for '$key'"
            val ref = reference.texts[key] ?: return@forEach
            val expected = TextCatalog.placeholders(ref)
            val actual = TextCatalog.placeholders(value)
            if (expected != actual) problems += "$name: placeholders of '$key' are $actual, expected $expected"
        }
        return problems
    }
}
