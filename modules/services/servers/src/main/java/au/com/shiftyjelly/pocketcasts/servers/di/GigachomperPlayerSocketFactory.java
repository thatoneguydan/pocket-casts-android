package au.com.shiftyjelly.pocketcasts.servers.di;

import android.net.Network;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import javax.net.SocketFactory;

/**
 * Delegating socket factory for the dedicated Pocket Cantrips player client.
 *
 * The delegate is selected for every new socket so the player can switch between the actual
 * matching home Wi-Fi Network socket factory and Android's ordinary socket factory without
 * recreating the singleton OkHttp client.
 */
final class GigachomperPlayerSocketFactory extends SocketFactory {
    private final GigachomperHomeLanDetector homeLanDetector;
    private final SocketFactory fallbackSocketFactory;

    GigachomperPlayerSocketFactory(
            GigachomperHomeLanDetector homeLanDetector,
            SocketFactory fallbackSocketFactory
    ) {
        this.homeLanDetector = homeLanDetector;
        this.fallbackSocketFactory = fallbackSocketFactory;
    }

    private SocketFactory currentSocketFactory() {
        try {
            Network homeNetwork = homeLanDetector.homeNetwork();
            if (homeNetwork != null) {
                return homeNetwork.getSocketFactory();
            }
        } catch (RuntimeException ignored) {
            // Preserve normal Android networking if network inspection races a transition.
        }
        return fallbackSocketFactory;
    }

    @Override
    public Socket createSocket() throws IOException {
        return currentSocketFactory().createSocket();
    }

    @Override
    public Socket createSocket(String host, int port) throws IOException {
        return currentSocketFactory().createSocket(host, port);
    }

    @Override
    public Socket createSocket(
            String host,
            int port,
            InetAddress localHost,
            int localPort
    ) throws IOException {
        return currentSocketFactory().createSocket(host, port, localHost, localPort);
    }

    @Override
    public Socket createSocket(InetAddress host, int port) throws IOException {
        return currentSocketFactory().createSocket(host, port);
    }

    @Override
    public Socket createSocket(
            InetAddress address,
            int port,
            InetAddress localAddress,
            int localPort
    ) throws IOException {
        return currentSocketFactory().createSocket(address, port, localAddress, localPort);
    }
}
