package com.thomasnorlund.archiveplayer.core.net

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
    val userAgent: String get() = TODO()
    fun get(url: String): String = TODO()

    companion object {
        val maxAttempts: Int get() = TODO()
    }
}

object NetworkPolicy {
    fun isAllowedHost(host: String): Boolean = TODO()
}
