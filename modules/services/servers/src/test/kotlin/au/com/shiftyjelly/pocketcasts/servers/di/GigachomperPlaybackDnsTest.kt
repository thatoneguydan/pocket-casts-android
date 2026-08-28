package au.com.shiftyjelly.pocketcasts.servers.di

import java.net.InetAddress
import okhttp3.Dns
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GigachomperPlaybackDnsTest {
    @Test
    fun `D&D host prefers Gigachomper and keeps system fallback on home LAN`() {
        val publicAddress = ipv4(76, 222, 77, 191)
        val fallback = RecordingDns(listOf(publicAddress))
        val dns = GigachomperPlaybackDns(
            isOnHomeLan = { true },
            fallbackDns = fallback,
        )

        val result = dns.lookup("dndkids.ddnsgeek.com")

        assertEquals(listOf(GigachomperPlaybackDns.LAN_ADDRESS, publicAddress), result)
        assertEquals(listOf("dndkids.ddnsgeek.com"), fallback.lookups)
    }

    @Test
    fun `D&D host uses system DNS away from home`() {
        val publicAddress = ipv4(76, 222, 77, 191)
        val fallback = RecordingDns(listOf(publicAddress))
        val dns = GigachomperPlaybackDns(
            isOnHomeLan = { false },
            fallbackDns = fallback,
        )

        val result = dns.lookup("dndkids.ddnsgeek.com")

        assertEquals(listOf(publicAddress), result)
        assertEquals(listOf("dndkids.ddnsgeek.com"), fallback.lookups)
    }

    @Test
    fun `other hosts always use system DNS`() {
        val publicAddress = ipv4(1, 1, 1, 1)
        val fallback = RecordingDns(listOf(publicAddress))
        val dns = GigachomperPlaybackDns(
            isOnHomeLan = { true },
            fallbackDns = fallback,
        )

        val result = dns.lookup("example.com")

        assertEquals(listOf(publicAddress), result)
        assertEquals(listOf("example.com"), fallback.lookups)
    }

    @Test
    fun `home detection failures fall back to system DNS`() {
        val publicAddress = ipv4(76, 222, 77, 191)
        val fallback = RecordingDns(listOf(publicAddress))
        val dns = GigachomperPlaybackDns(
            isOnHomeLan = { error("network state unavailable") },
            fallbackDns = fallback,
        )

        val result = dns.lookup("dndkids.ddnsgeek.com")

        assertEquals(listOf(publicAddress), result)
    }

    @Test
    fun `home matcher requires wifi address and BGW320 gateway`() {
        val localAddress = ipv4(192, 168, 1, 42)
        val gateway = ipv4(192, 168, 1, 254)

        assertTrue(
            GigachomperHomeLanMatcher.matches(
                isWifi = true,
                localAddresses = listOf(localAddress),
                defaultGateways = listOf(gateway),
            ),
        )

        assertFalse(
            GigachomperHomeLanMatcher.matches(
                isWifi = false,
                localAddresses = listOf(localAddress),
                defaultGateways = listOf(gateway),
            ),
        )
        assertFalse(
            GigachomperHomeLanMatcher.matches(
                isWifi = true,
                localAddresses = listOf(ipv4(192, 168, 0, 42)),
                defaultGateways = listOf(gateway),
            ),
        )
        assertFalse(
            GigachomperHomeLanMatcher.matches(
                isWifi = true,
                localAddresses = listOf(localAddress),
                defaultGateways = listOf(ipv4(192, 168, 1, 1)),
            ),
        )
    }

    private fun ipv4(first: Int, second: Int, third: Int, fourth: Int): InetAddress {
        return InetAddress.getByAddress(
            byteArrayOf(first.toByte(), second.toByte(), third.toByte(), fourth.toByte()),
        )
    }

    private class RecordingDns(private val result: List<InetAddress>) : Dns {
        val lookups = mutableListOf<String>()

        override fun lookup(hostname: String): List<InetAddress> {
            lookups += hostname
            return result
        }
    }
}
