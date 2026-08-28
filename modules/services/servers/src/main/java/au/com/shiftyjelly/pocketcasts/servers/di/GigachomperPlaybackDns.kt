package au.com.shiftyjelly.pocketcasts.servers.di

import android.content.Context
import android.net.ConnectivityManager
import android.net.LinkProperties
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
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
 * Finds the actual Android Network representing the known home Wi-Fi LAN.
 *
 * This intentionally scans all currently connected networks instead of trusting activeNetwork.
 * A long-lived Android process can temporarily retain a different default-network view while the
 * home Wi-Fi is already attached. Binding player sockets to the matching Network avoids depending
 * on that process/default-network timing.
 */
internal class GigachomperHomeLanDetector(context: Context) {
    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager

    fun isOnHomeLan(): Boolean = homeNetwork() != null

    fun homeNetwork(): Network? {
        val manager = connectivityManager ?: return null

        return manager.allNetworks.firstOrNull { network ->
            val capabilities = manager.getNetworkCapabilities(network) ?: return@firstOrNull false
            val linkProperties = manager.getLinkProperties(network) ?: return@firstOrNull false

            GigachomperHomeLanMatcher.matches(
                isWifi = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI),
                localAddresses = linkProperties.linkAddresses.map { it.address },
                defaultGateways = linkProperties.routes
                    .filter { it.isDefaultRoute }
                    .mapNotNull { it.gateway },
            )
        }
    }
}

/**
 * Watches Wi-Fi network state rather than only Android's default network. Every relevant Wi-Fi
 * availability, loss, capability, or link-property change invalidates only the isolated player
 * connection pool. The next player request then creates a fresh socket and re-evaluates both the
 * home-LAN DNS preference and the matching Android Network binding.
 */
internal class GigachomperPlaybackNetworkObserver(
    context: Context,
    private val onWifiNetworkChanged: () -> Unit,
) {
    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
    private val callback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            onWifiNetworkChanged()
        }

        override fun onLost(network: Network) {
            onWifiNetworkChanged()
        }

        override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
            onWifiNetworkChanged()
        }

        override fun onLinkPropertiesChanged(network: Network, linkProperties: LinkProperties) {
            onWifiNetworkChanged()
        }
    }

    fun start() {
        runCatching {
            val request = NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .build()
            connectivityManager?.registerNetworkCallback(request, callback)
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
