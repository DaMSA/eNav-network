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
import java.util.concurrent.TimeUnit;

import javax.websocket.ContainerProvider;
import javax.websocket.DeploymentException;
import javax.websocket.WebSocketContainer;

import org.eclipse.jetty.util.component.ContainerLifeCycle;
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
    volatile WebSocketContainer container;

    /**
     * Creates a new ClientTransportFactory.
     * 
     * @param uri
     *            the uri to connect to
     */
    private TransportClientFactory(URI uri) {
        this.uri = requireNonNull(uri);
    }

    private synchronized WebSocketContainer lazyInitialize() {
        WebSocketContainer container = this.container;
        if (container == null) {
            return this.container = ContainerProvider.getWebSocketContainer();
        }
        return container;
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
        TransportListener client = new TransportListener(listener);
        long now = System.nanoTime();
        LOG.info("Connecting to " + uri);
        try {
            lazyInitialize().connectToServer(client, uri);// . .get(timeout, unit);
        } catch (DeploymentException e) {
            throw new IOException(e);
        }
        long remaining = unit.toNanos(timeout) - (System.nanoTime() - now);
        try {
            // TODO check return status
            // TODO Make sure transport is not used again. Its invalid
            client.connectedLatch.await(remaining, TimeUnit.NANOSECONDS);
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
            ((ContainerLifeCycle) container).stop();
        } catch (Exception e) {
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
