package au.com.shiftyjelly.pocketcasts.servers.di;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.function.Supplier;
import javax.net.SocketFactory;

/**
 * Delegating socket factory for the dedicated Pocket Cantrips player client.
 *
 * The delegate is selected for every new socket so the player can switch between the actual
 * matching home Wi-Fi Network socket factory and Android's ordinary socket factory without
 * recreating the singleton OkHttp client.
 */
final class GigachomperPlayerSocketFactory extends SocketFactory {
    private final Supplier<SocketFactory> socketFactorySupplier;

    GigachomperPlayerSocketFactory(Supplier<SocketFactory> socketFactorySupplier) {
        this.socketFactorySupplier = socketFactorySupplier;
    }

    private SocketFactory currentSocketFactory() {
        SocketFactory socketFactory = socketFactorySupplier.get();
        if (socketFactory == null) {
            throw new IllegalStateException("Player socket factory supplier returned null");
        }
        return socketFactory;
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
