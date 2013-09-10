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
import java.util.concurrent.CountDownLatch;

import org.eclipse.jetty.websocket.api.RemoteEndpoint;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WebSocketListener;

import dk.dma.enav.communication.ClosingCode;

/**
 * The listener interacting with Jettys websocket library.
 * 
 * @author Kasper Nielsen
 */
final class TransportWebSocketListener implements WebSocketListener, SomeListener {

    /** A latch that is released when we receive a connected message from the remote end. */
    final CountDownLatch connected = new CountDownLatch(1);

    /** The currently connected session. */
    volatile Session session;

    /** The upstream protocol layer. */
    private final Transport transport;

    /**
     * Creates a new listener.
     * 
     * @param transport
     *            the upstream protocol layer
     */
    TransportWebSocketListener(Transport transport) {
        this.transport = requireNonNull(transport);
    }

    /** {@inheritDoc} */
    public final void close(ClosingCode reason) {
        Session s = session;
        try {
            if (s != null) {
                s.close(reason.getId(), reason.getMessage());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /** {@inheritDoc} */
    @Override
    public final void onWebSocketBinary(byte[] payload, int offset, int len) {
        Session s = session;
        try {
            ClosingCode r = ClosingCode.create(ClosingCode.BAD_DATA.getId(), "Expected text only");
            s.close(r.getId(), r.getMessage());
            transport.closedByWebsocket(r);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /** {@inheritDoc} */
    @Override
    public final void onWebSocketClose(int statusCode, String reason) {
        session = null;
        transport.closedByWebsocket(ClosingCode.create(statusCode, reason));
    }

    /** {@inheritDoc} */
    @Override
    public final void onWebSocketConnect(Session session) {
        this.session = session;
        connected.countDown();
        transport.setSession(this);
    }

    /** {@inheritDoc} */
    @Override
    public final void onWebSocketError(Throwable throwable) {
        // The remote end send us an invalid websocket packet. Will close the connection
    }

    /** {@inheritDoc} */
    @Override
    public final void onWebSocketText(String message) {
        try {
            transport.rawReceive(message);
        } catch (Exception e) {
            // close connection, for example parse error
        }
    }

    /** {@inheritDoc} */
    public final void sendText(String text) {
        Session s = session;
        RemoteEndpoint r = s == null ? null : s.getRemote();
        if (r != null) {
            try {
                r.sendString(text);
            } catch (IOException e) {
                e.printStackTrace();
                // We nede to reconnect
            }
        }
    }
}
