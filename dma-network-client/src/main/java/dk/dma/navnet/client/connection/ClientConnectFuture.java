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
package dk.dma.navnet.client.connection;

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.dma.enav.maritimecloud.ClosingCode;
import dk.dma.enav.maritimecloud.MaritimeCloudConnection.Listener;
import dk.dma.enav.model.geometry.PositionTime;
import dk.dma.navnet.client.ClientContainer;
import dk.dma.navnet.messages.TransportMessage;
import dk.dma.navnet.messages.auxiliary.ConnectedMessage;
import dk.dma.navnet.messages.auxiliary.HelloMessage;
import dk.dma.navnet.messages.auxiliary.WelcomeMessage;

/**
 * This class takes of connecting and handshaking with a remote server.
 * 
 * @author Kasper Nielsen
 */
class ClientConnectFuture implements Runnable {

    /** The logger. */
    private static final Logger LOG = LoggerFactory.getLogger(ClientConnectFuture.class);

    private final ClientConnection connection;

    private final long reconnectId;

    private final CountDownLatch cancelled = new CountDownLatch(1);

    private final ClientTransport transport;

    private boolean receivedHelloMessage = false;

    /** The thread during the actual connect. */
    private volatile Thread thread;

    String connectedId;

    ClientConnectFuture(ClientConnection connection, long reconnectId) {
        this.connection = requireNonNull(connection);
        this.reconnectId = reconnectId;
        transport = new JavaxWebsocketTransport(this, connection);
    }

    /** {@inheritDoc} */
    @Override
    public void run() {
        ConnectionManager cm = connection.connectionManager;
        thread = Thread.currentThread();
        while (cancelled.getCount() > 0) {
            try {
                transport.connect(cm.uri);
                return;
            } catch (IllegalStateException e) {
                LOG.error("A serious internal error", e);
            } catch (IOException e) {
                if (cancelled.getCount() > 0) {// Only log the error if we are not cancelled
                    LOG.error("A serious internal error", e);
                }
            }
            try {
                cancelled.await(1, TimeUnit.SECONDS); // exponential backoff?
            } catch (InterruptedException ignore) {}
        }
    }

    /**
     * Takes care of the connection handshake.
     * 
     * @param m
     *            the message we have received
     */
    void onMessage(TransportMessage m) {
        if (!receivedHelloMessage) {
            if (m instanceof WelcomeMessage) {
                ClientContainer client = connection.connectionManager.client;
                PositionTime pt = client.readCurrentPosition();
                String connectName = connection.connectionId == null ? "" : connection.connectionId;
                transport.sendText(new HelloMessage(client.getLocalId(), "enavClient/1.0", connectName, reconnectId, pt
                        .getLatitude(), pt.getLongitude()).toJSON());
                receivedHelloMessage = true;
            } else {
                String err = "Expected a welcome message, but was: " + m.getClass().getSimpleName();
                LOG.error(err);
                transport.doClose(ClosingCode.WRONG_MESSAGE.withMessage(err));
            }
        } else {
            if (m instanceof ConnectedMessage) {
                ConnectedMessage cm = (ConnectedMessage) m;
                boolean isReconnected = Objects.equals(cm.getConnectionId(), connection.connectionId);
                connection.connectionId = cm.getConnectionId();
                // if (cm.getLastReceivedMessageId() >= 0) {
                // List<OutstandingMessage> os = connection.rq.reConnected(cm);
                //
                // for (OutstandingMessage o : os) {
                // transport.sendText(o.msg);
                // }
                // // Okay lets send the outstanding message
                // }
                connectedId = cm.getConnectionId();
                connection.connected(this, transport);

                connection.worker.onConnect(transport, cm.getLastReceivedMessageId(), isReconnected);
                // We need to retransmit messages
                transport.connectFuture = null; // make sure we do not get any more messages
                for (Listener l : connection.connectionManager.listeners) {
                    l.connected();
                }
            } else {
                String err = "Expected a connected message, but was: " + m.getClass().getSimpleName();
                LOG.error(err);
                transport.doClose(ClosingCode.WRONG_MESSAGE.withMessage(err));
            }
        }
    }

    void cancelConnectUnderLock() {
        cancelled.countDown();
        Thread t = thread;
        if (t != null) {
            t.interrupt();
            transport.doClose(ClosingCode.CONNECT_CANCELLED.withMessage("connect cancelled"));
            t = null; // only invoke once
        }
    }
}
