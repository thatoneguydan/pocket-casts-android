package au.com.shiftyjelly.pocketcasts.servers.di

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import java.net.InetAddress
import okhttp3.Dns

/**
 * Routes the private D&D podcast host directly to Gigachomper while the device is on the
 * known home LAN. The request URL and hostname stay unchanged, so HTTPS/SNI/certificate
 * verification continue to use dndkids.ddnsgeek.com.
 *
 * System DNS results are kept after the LAN address as a fail-safe. On the home network the
 * LAN socket wins immediately; elsewhere the normal resolver is used without modification.
 */
internal class GigachomperPlaybackDns(
    private val isOnHomeLan: () -> Boolean,
    private val fallbackDns: Dns = Dns.SYSTEM,
) : Dns {
    override fun lookup(hostname: String): List<InetAddress> {
        val shouldPreferGigachomper = hostname.equals(PUBLIC_HOST, ignoreCase = true) &&
            runCatching(isOnHomeLan).getOrDefault(false)

        if (!shouldPreferGigachomper) {
            return fallbackDns.lookup(hostname)
        }

        val fallbackAddresses = fallbackDns.lookup(hostname)
        return buildList {
            add(LAN_ADDRESS)
            fallbackAddresses.forEach { address ->
                if (address != LAN_ADDRESS) {
                    add(address)
                }
            }
        }
    }

    companion object {
        internal const val PUBLIC_HOST = "dndkids.ddnsgeek.com"
        internal val LAN_ADDRESS: InetAddress = InetAddress.getByAddress(
            byteArrayOf(192.toByte(), 168.toByte(), 1, 250.toByte()),
        )
    }
}

/**
 * Identifies the home LAN without SSID/location access. The match is intentionally strict:
 * active transport must be Wi-Fi, the device must have a 192.168.1.x IPv4 address, and the
 * active network's default gateway must be the AT&T BGW320 at 192.168.1.254.
 */
internal class GigachomperHomeLanDetector(context: Context) {
    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager

    fun isOnHomeLan(): Boolean {
        val manager = connectivityManager ?: return false
        val network = manager.activeNetwork ?: return false
        val capabilities = manager.getNetworkCapabilities(network) ?: return false
        val linkProperties = manager.getLinkProperties(network) ?: return false

        return GigachomperHomeLanMatcher.matches(
            isWifi = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI),
            localAddresses = linkProperties.linkAddresses.map { it.address },
            defaultGateways = linkProperties.routes
                .filter { it.isDefaultRoute }
                .mapNotNull { it.gateway },
        )
    }
}

/**
 * Watches Android's default-network identity for the lifetime of the singleton player client.
 * When the default network changes, callers invalidate only the player's connection pool so a
 * later request cannot reuse a route selected on the previous network.
 */
internal class GigachomperPlaybackNetworkObserver(
    context: Context,
    onDefaultNetworkChanged: () -> Unit,
) {
    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
    private val transitionTracker = GigachomperDefaultNetworkTracker(onDefaultNetworkChanged)
    private val callback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            transitionTracker.onAvailable(network.networkHandle)
        }

        override fun onLost(network: Network) {
            transitionTracker.onLost(network.networkHandle)
        }
    }

    fun start() {
        runCatching {
            connectivityManager?.registerDefaultNetworkCallback(callback)
        }
    }
}

/**
 * Pure state machine kept separate from Android callbacks so transition semantics stay unit-testable.
 * Initial network discovery is not considered a transition; moving away from an established default
 * network is.
 */
internal class GigachomperDefaultNetworkTracker(
    private val onDefaultNetworkChanged: () -> Unit,
) {
    private var currentNetworkHandle: Long? = null

    @Synchronized
    fun onAvailable(networkHandle: Long) {
        val previousHandle = currentNetworkHandle
        currentNetworkHandle = networkHandle

        if (previousHandle != null && previousHandle != networkHandle) {
            onDefaultNetworkChanged()
        }
    }

    @Synchronized
    fun onLost(networkHandle: Long) {
        if (currentNetworkHandle == networkHandle) {
            currentNetworkHandle = null
            onDefaultNetworkChanged()
        }
    }
}

internal object GigachomperHomeLanMatcher {
    fun matches(
        isWifi: Boolean,
        localAddresses: Iterable<InetAddress>,
        defaultGateways: Iterable<InetAddress>,
    ): Boolean {
        if (!isWifi) {
            return false
        }

        val hasHomeIpv4Address = localAddresses.any { address ->
            address.matchesIpv4Prefix(192, 168, 1)
        }
        val hasHomeGateway = defaultGateways.any { address ->
            address.matchesIpv4(192, 168, 1, 254)
        }

        return hasHomeIpv4Address && hasHomeGateway
    }

    private fun InetAddress.matchesIpv4Prefix(first: Int, second: Int, third: Int): Boolean {
        val bytes = address
        return bytes.size == 4 &&
            bytes[0].toUnsignedInt() == first &&
            bytes[1].toUnsignedInt() == second &&
            bytes[2].toUnsignedInt() == third
    }

    private fun InetAddress.matchesIpv4(first: Int, second: Int, third: Int, fourth: Int): Boolean {
        val bytes = address
        return bytes.size == 4 &&
            bytes[0].toUnsignedInt() == first &&
            bytes[1].toUnsignedInt() == second &&
            bytes[2].toUnsignedInt() == third &&
            bytes[3].toUnsignedInt() == fourth
    }

    private fun Byte.toUnsignedInt(): Int = toInt() and 0xff
}
