package com.thomasnorlund.archiveplayer.core.net

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class ArchiveClientTest {
    private class FakeTransport(vararg responses: HttpResponse) : HttpTransport {
        private val queue = ArrayDeque(responses.toList())
        val calls = mutableListOf<Pair<String, Map<String, String>>>()
        override fun get(url: String, headers: Map<String, String>): HttpResponse {
            calls += url to headers
            return if (queue.size > 1) queue.removeFirst() else queue.first()
        }
    }

    private val sleeps = mutableListOf<Long>()
    private fun client(t: HttpTransport) = ArchiveClient(t, { sleeps += it }, "1.2.3")
    private val url = "https://archive.org/metadata/x"

    @Test fun R76_descriptiveUserAgentSent() {
        val t = FakeTransport(HttpResponse(200, "ok"))
        val c = client(t)
        assertTrue(c.userAgent.contains("ArchivePlayer/1.2.3"))
        assertTrue(c.userAgent.contains("github.com/jutlandiatech/ArchivePlayer"))
        assertEquals("ok", c.get(url))
        assertEquals(c.userAgent, t.calls.single().second["User-Agent"])
    }

    @Test fun A35_backsOffExponentiallyThenSucceeds() {
        val t = FakeTransport(HttpResponse(429, ""), HttpResponse(429, ""), HttpResponse(200, "body"))
        assertEquals("body", client(t).get(url))
        assertEquals(3, t.calls.size)
        assertEquals(2, sleeps.size)
        assertTrue(sleeps.toString(), sleeps[1] > sleeps[0] && sleeps[0] > 0)
    }

    @Test fun A35_noRequestStorm() {
        val t = FakeTransport(HttpResponse(429, ""))
        assertThrows(RateLimitedException::class.java) { client(t).get(url) }
        assertEquals(ArchiveClient.maxAttempts, t.calls.size)
        assertTrue(ArchiveClient.maxAttempts in 2..4)
    }

    @Test fun R76_honoursRetryAfter() {
        val t = FakeTransport(HttpResponse(429, "", mapOf("Retry-After" to "5")), HttpResponse(200, "ok"))
        client(t).get(url)
        assertEquals(listOf(5000L), sleeps)
    }

    @Test fun R76_serverErrorsRetriedClientErrorsNot() {
        assertEquals("ok", client(FakeTransport(HttpResponse(503, ""), HttpResponse(200, "ok"))).get(url))
        val t = FakeTransport(HttpResponse(404, ""))
        val e = assertThrows(HttpStatusException::class.java) { client(t).get(url) }
        assertEquals(404, e.code)
        assertEquals(1, t.calls.size)
    }

    @Test fun R75_foreignHostsBlocked() {
        val t = FakeTransport(HttpResponse(200, "ok"))
        assertThrows(BlockedHostException::class.java) { client(t).get("https://tracker.example.com/x") }
        assertTrue(t.calls.isEmpty())
    }

    @Test fun R75_allowList() {
        listOf("archive.org", "ia800300.us.archive.org", "dn720003.ca.archive.org", "jutlandiatech.github.io")
            .forEach { assertTrue(it, NetworkPolicy.isAllowedHost(it)) }
        listOf("evil.github.io", "archive.org.evil.com", "notarchive.org", "example.com")
            .forEach { assertFalse(it, NetworkPolicy.isAllowedHost(it)) }
    }
}
