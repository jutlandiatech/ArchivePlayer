package com.jutlandiatech.archiveplayer.core.net

import java.net.URI

data class HttpResponse(val code: Int, val body: String, val headers: Map<String, String> = emptyMap())

fun interface HttpTransport {
    fun get(url: String, headers: Map<String, String>): HttpResponse
}

fun interface Sleeper {
    fun sleep(millis: Long)
}

class RateLimitedException(val retryAfterMillis: Long?) : Exception("archive.org rate limit")
class HttpStatusException(val code: Int) : Exception("HTTP $code")
class BlockedHostException(val host: String) : Exception("Host not allowed: $host")

/** Polite archive.org client: User-Agent, bounded exponential back-off on 429/5xx, host allow-list (R75, R76, A35). */
class ArchiveClient(
    private val transport: HttpTransport,
    private val sleeper: Sleeper,
    private val appVersion: String,
) {
    val userAgent: String get() = "ArchivePlayer/$appVersion (+https://github.com/jutlandiatech/ArchivePlayer)"

    fun get(url: String): String {
        val host = runCatching { URI(url).host }.getOrNull().orEmpty()
        if (!NetworkPolicy.isAllowedHost(host)) throw BlockedHostException(host)
        var attempt = 0
        while (true) {
            attempt++
            val response = transport.get(url, mapOf("User-Agent" to userAgent))
            val code = response.code
            if (code in 200..299) return response.body
            val retryable = code == 429 || code in 500..599
            val retryAfter = response.headers.entries.firstOrNull { it.key.equals("Retry-After", ignoreCase = true) }
                ?.value?.trim()?.toLongOrNull()?.times(1000)
            if (!retryable || attempt >= maxAttempts) {
                throw if (code == 429) RateLimitedException(retryAfter) else HttpStatusException(code)
            }
            sleeper.sleep(retryAfter ?: (BASE_DELAY_MS shl (attempt - 1)))
        }
    }

    companion object {
        private const val BASE_DELAY_MS = 1000L
        val maxAttempts: Int get() = 3
    }
}

object NetworkPolicy {
    private val exactHosts = setOf("archive.org", "jutlandiatech.github.io")

    /** Extra hosts allowed in instrumented tests (local fixture server). */
    @Volatile var testHosts: Set<String> = emptySet()

    fun isAllowedHost(host: String): Boolean {
        val h = host.lowercase().trimEnd('.')
        return h in exactHosts || h.endsWith(".archive.org") || h in testHosts
    }
}
