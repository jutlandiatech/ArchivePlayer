package com.thomasnorlund.archiveplayer.ui

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import com.thomasnorlund.archiveplayer.core.text.TextCatalog

/** All shipped TextCatalog files keyed by ISO code (R65). */
class AppTexts(val catalogs: Map<String, TextCatalog>) {
    val isoCodes: List<String> get() = catalogs.keys.toList()
    fun forLanguage(iso: String): TextCatalog = catalogs[iso] ?: catalogs.getValue("en")

    companion object {
        fun load(context: Context): AppTexts {
            val assets = context.assets
            val files = assets.list("texts").orEmpty().sorted()
            val catalogs = files.mapNotNull { name ->
                TextCatalog.codeFromFileName(name)?.let { code ->
                    TextCatalog.parse(code, assets.open("texts/$name").bufferedReader().use { it.readText() })
                }
            }.sortedBy { listOf("en", "de", "da").indexOf(it.isoCode).let { i -> if (i < 0) 99 else i } }
            return AppTexts(catalogs.associateBy { it.isoCode })
        }
    }
}

val LocalTexts = staticCompositionLocalOf<TextCatalog> { error("No TextCatalog provided") }

@Composable
fun t(key: String, vararg args: Pair<String, Any>): String = LocalTexts.current.text(key, args.toMap())
