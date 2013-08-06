/* Copyright (c) 2011 Danish Maritime Authority
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this library.  If not, see <http://www.gnu.org/licenses/>.
 */
package dk.dma.navnet.protocol.transport;

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.eclipse.jetty.websocket.client.WebSocketClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A factory used to create transports by connecting to a remote server.
 * 
 * @author Kasper Nielsen
 */
public final class TransportClientFactory {

    /** The logger. */
    static final Logger LOG = LoggerFactory.getLogger(TransportClientFactory.class);

    /** The URI to connect to. Is constant. */
    private final URI uri;

    /** The actual WebSocket client. Changes when reconnecting. */
    volatile WebSocketClient client;

    /**
     * Creates a new ClientTransportFactory.
     * 
     * @param uri
     *            the uri to connect to
     */
    private TransportClientFactory(URI uri) {
        this.uri = requireNonNull(uri);
    }

    private synchronized WebSocketClient lazyInitialize() throws IOException {
        if (client == null) {
            WebSocketClient client = new WebSocketClient();
            try {
                client.start();
            } catch (Exception e) {
                throw new IOException(e);
            }
            this.client = client;// only set if it could be succesfully started
        }
        return client;
    }

    /**
     * Connects using the specified transport.
     * 
     * @param transport
     *            the transport to use
     * @param timeout
     *            the connection time out
     * @param unit
     *            the unit of timeout
     * @throws IOException
     *             could not connect
     */
    public void connect(Transport listener, long timeout, TimeUnit unit) throws IOException {
        TransportWebSocketListener client = new TransportWebSocketListener(listener);
        long now = System.nanoTime();
        LOG.info("Connecting to " + uri);
        try {
            lazyInitialize().connect(client, uri).get(timeout, unit);
        } catch (InterruptedException e) {
            throw new InterruptedIOException();
        } catch (ExecutionException e) {
            if (e.getCause() instanceof IOException) {
                throw (IOException) e.getCause();
            }
            throw new IOException(e);
        } catch (TimeoutException e) {
            throw new IOException("Connect timed out", e);
        }
        long remaining = unit.toNanos(timeout) - (System.nanoTime() - now);
        try {
            // TODO check return status
            // TODO Make sure transport is not used again. Its invalid
            client.connected.await(remaining, TimeUnit.NANOSECONDS);
        } catch (InterruptedException e) {
            throw new InterruptedIOException();
        }
    }

    /**
     * Shuts down the factory.
     * 
     * @throws IOException
     */
    public void shutdown() throws IOException {
        try {
            client.stop();
        } catch (Exception e) {
            if (e.getCause() instanceof IOException) {
                throw (IOException) e.getCause();
            }
            throw new IOException(e);
        }
    }

    public static TransportClientFactory createClient(String hostPort) {
        requireNonNull(hostPort);
        try {
            return new TransportClientFactory(new URI("ws://" + hostPort));
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(e);
        }
    }
}
