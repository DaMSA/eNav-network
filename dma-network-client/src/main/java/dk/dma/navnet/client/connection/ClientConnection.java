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
import java.io.InterruptedIOException;
import java.net.ConnectException;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import dk.dma.enav.communication.ClosingCode;
import dk.dma.enav.communication.MaritimeNetworkConnectionBuilder;
import dk.dma.enav.util.function.Consumer;
import dk.dma.navnet.client.ClientInfo;
import dk.dma.navnet.client.broadcast.BroadcastManager;
import dk.dma.navnet.client.util.DefaultConnectionFuture;
import dk.dma.navnet.client.util.ThreadManager;
import dk.dma.navnet.messages.ConnectionMessage;
import dk.dma.navnet.messages.s2c.service.FindServiceResult;
import dk.dma.navnet.messages.s2c.service.RegisterServiceResult;

/**
 * 
 * @author Kasper Nielsen
 */
public class ClientConnection extends AbstractClientConnection {

    BroadcastManager broadcaster;

    volatile String connectionId;

    private ArrayList<MessageConsumer> list = new ArrayList<>();

    /** Factory for creating new transports. */
    final TransportClientFactory transportFactory;

    public ClientConnection(ClientInfo clientInfo, ThreadManager threadManager, MaritimeNetworkConnectionBuilder b) {
        super("fff", threadManager);
        this.transportFactory = TransportClientFactory.createClient(b.getHost());
        super.setTransport(new ClientTransport(clientInfo, b.getPositionSupplier()));
    }

    public void connect(long timeout, TimeUnit unit) throws IOException {
        try {
            transportFactory.connect(getTransport(), timeout, unit);
            if (!((ClientTransport) getTransport()).awaitFullyConnected(timeout, unit)) {
                throw new ConnectException("Timedout while connecting to ");
            }
        } catch (IOException e) {
            threadManager.stop();
            throw e;
        } catch (InterruptedException e) {
            throw new InterruptedIOException();
        }
    }

    void disconnectOops(ClosingCode cr) {
        if (cr.getId() == 1000) {
            // cm.close();
        } else {
            // Try to reconnect
        }

    }

    /**
     * @return the connectionId
     */
    // Tjah ved ikke lige hvad forskellen er
    public String getConnectionId2() {
        return connectionId;
    }

    /** {@inheritDoc} */
    public final void handleMessage(ConnectionMessage m) {
        for (MessageConsumer c : list) {
            if (c.type.isAssignableFrom(m.getClass())) {
                c.c.accept(m);
            }
        }
        // System.err.println("Received an unknown message " + m.getReceivedRawMesage());
    }

    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    @Override
    protected final void handleMessageReply(ConnectionMessage m, DefaultConnectionFuture<?> f) {
        if (m instanceof RegisterServiceResult) {
            serviceRegisteredAck((RegisterServiceResult) m, (DefaultConnectionFuture<RegisterServiceResult>) f);
        } else if (m instanceof FindServiceResult) {
            serviceFindAck((FindServiceResult) m, (DefaultConnectionFuture<FindServiceResult>) f);
        } else {
            // unknownMessage(m);
        }
    }

    /** {@inheritDoc} */
    protected void serviceFindAck(FindServiceResult a, DefaultConnectionFuture<FindServiceResult> f) {
        f.complete(a);
    }

    /** {@inheritDoc} */
    protected void serviceRegisteredAck(RegisterServiceResult a, DefaultConnectionFuture<RegisterServiceResult> f) {
        f.complete(a);
    }

    @SuppressWarnings("unchecked")
    public <T extends ConnectionMessage> void subscribe(Class<T> type, Consumer<? super T> c) {
        list.add(new MessageConsumer(type, (Consumer<ConnectionMessage>) c));
    }

    static class MessageConsumer {
        final Consumer<ConnectionMessage> c;
        final Class<?> type;

        MessageConsumer(Class<?> type, Consumer<ConnectionMessage> c) {
            this.type = requireNonNull(type);
            this.c = requireNonNull(c);
        }
    }
}
