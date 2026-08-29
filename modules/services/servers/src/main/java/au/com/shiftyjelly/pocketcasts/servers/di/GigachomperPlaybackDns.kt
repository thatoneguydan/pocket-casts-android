package au.com.shiftyjelly.pocketcasts.servers.di

import android.content.Context
import android.net.ConnectivityManager
import android.net.LinkProperties
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import java.net.InetAddress
import java.net.InetSocketAddress
import okhttp3.Dns

/**
 * Routes the private D&D podcast host directly to Gigachomper while the device is on the
 * known home LAN. The request URL and hostname stay unchanged, so HTTPS/SNI/certificate
 * verification continue to use dndkids.ddnsgeek.com.
 *
 * When home Wi-Fi is confirmed, only the Gigachomper LAN address is returned. Away from home,
 * normal system DNS remains unchanged. Keeping the two paths exclusive prevents the known-slow
 * BGW320 NAT-loopback address from winning a player connection while the phone is at home.
 */
internal class GigachomperPlaybackDns(
    private val isOnHomeLan: () -> Boolean,
    private val fallbackDns: Dns = Dns.SYSTEM,
) : Dns {
    override fun lookup(hostname: String): List<InetAddress> {
        val shouldUseGigachomper = hostname.equals(PUBLIC_HOST, ignoreCase = true) &&
            runCatching(isOnHomeLan).getOrDefault(false)

        if (!shouldUseGigachomper) {
            return fallbackDns.lookup(hostname)
        }

        return listOf(LAN_ADDRESS)
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
 * A long-lived Android process can temporarily retain a stale default-network view after moving
 * between cellular and Wi-Fi. The Wi-Fi callback therefore records every attached Wi-Fi Network
 * while activeNetwork remains an immediate cold-start fallback.
 *
 * Android can report a newly available Wi-Fi Network before its complete LinkProperties/default
 * route are visible. The strict subnet + BGW320-gateway fingerprint remains the fast path, but a
 * short network-bound TCP probe to Gigachomper confirms home Wi-Fi during that transition window.
 * This avoids a false negative that would otherwise send the first player request through the
 * approximately 15-second public NAT-loopback path.
 *
 * Public visibility is limited to Java interoperability for the adjacent dynamic socket factory;
 * this remains an implementation detail of the servers module.
 */
class GigachomperHomeLanDetector(context: Context) {
    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager

    fun isOnHomeLan(): Boolean = homeNetwork() != null

    fun homeNetwork(): Network? {
        val manager = connectivityManager ?: return null
        val candidates = GigachomperWifiNetworkRegistry.snapshot().toMutableList()

        manager.activeNetwork?.let { activeNetwork ->
            if (activeNetwork !in candidates) {
                candidates.add(activeNetwork)
            }
        }

        return candidates.firstOrNull { network ->
            val capabilities = manager.getNetworkCapabilities(network) ?: return@firstOrNull false
            if (!capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                return@firstOrNull false
            }

            val linkProperties = manager.getLinkProperties(network)
            val fingerprintMatches = linkProperties?.let { properties ->
                GigachomperHomeLanMatcher.matches(
                    isWifi = true,
                    localAddresses = properties.linkAddresses.map { it.address },
                    defaultGateways = properties.routes
                        .filter { it.isDefaultRoute }
                        .mapNotNull { it.gateway },
                )
            } == true

            fingerprintMatches || GigachomperHomeLanProbe.canReachGigachomper(network)
        }
    }
}

/**
 * Confirms Gigachomper through one specific Android Network without changing process-wide routing.
 *
 * This is only a transition fallback after Android has already identified the candidate as Wi-Fi.
 * The probe is intentionally short and targets only Gigachomper's HTTPS port. The real media
 * request still performs normal HTTPS hostname and certificate validation.
 */
internal object GigachomperHomeLanProbe {
    private const val HTTPS_PORT = 443
    private const val CONNECT_TIMEOUT_MS = 175
    private const val RETRY_DELAY_MS = 50L
    private const val ATTEMPTS = 3

    fun canReachGigachomper(network: Network): Boolean {
        repeat(ATTEMPTS) { attempt ->
            val connected = runCatching {
                network.socketFactory.createSocket().use { socket ->
                    socket.connect(
                        InetSocketAddress(GigachomperPlaybackDns.LAN_ADDRESS, HTTPS_PORT),
                        CONNECT_TIMEOUT_MS,
                    )
                    socket.isConnected
                }
            }.getOrDefault(false)

            if (connected) {
                return true
            }

            if (attempt < ATTEMPTS - 1) {
                runCatching { Thread.sleep(RETRY_DELAY_MS) }
            }
        }

        return false
    }
}

/**
 * Watches Wi-Fi network state rather than only Android's default network. The callback also keeps
 * a process-local registry of currently attached Wi-Fi Network objects. Every relevant change
 * invalidates only the isolated player connection pool; the next player request then re-evaluates
 * both the home-LAN DNS preference and the matching Android Network binding.
 */
internal class GigachomperPlaybackNetworkObserver(
    context: Context,
    private val onWifiNetworkChanged: () -> Unit,
) {
    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
    private val callback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            GigachomperWifiNetworkRegistry.add(network)
            onWifiNetworkChanged()
        }

        override fun onLost(network: Network) {
            GigachomperWifiNetworkRegistry.remove(network)
            onWifiNetworkChanged()
        }

        override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
            GigachomperWifiNetworkRegistry.add(network)
            onWifiNetworkChanged()
        }

        override fun onLinkPropertiesChanged(network: Network, linkProperties: LinkProperties) {
            GigachomperWifiNetworkRegistry.add(network)
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

internal object GigachomperWifiNetworkRegistry {
    private val lock = Any()
    private val wifiNetworks = linkedSetOf<Network>()

    fun add(network: Network) {
        synchronized(lock) {
            wifiNetworks.add(network)
        }
    }

    fun remove(network: Network) {
        synchronized(lock) {
            wifiNetworks.remove(network)
        }
    }

    fun snapshot(): List<Network> = synchronized(lock) {
        wifiNetworks.toList()
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
