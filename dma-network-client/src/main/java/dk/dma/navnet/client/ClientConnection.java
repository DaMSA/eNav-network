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
package dk.dma.navnet.client;

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.ConnectException;
import java.util.concurrent.TimeUnit;

import dk.dma.navnet.client.util.DefaultConnectionFuture;
import dk.dma.navnet.core.messages.ConnectionMessage;
import dk.dma.navnet.core.messages.TransportMessage;
import dk.dma.navnet.core.messages.c2c.broadcast.BroadcastMsg;
import dk.dma.navnet.core.messages.c2c.service.InvokeService;
import dk.dma.navnet.core.messages.c2c.service.InvokeServiceResult;
import dk.dma.navnet.core.messages.s2c.service.FindServiceResult;
import dk.dma.navnet.core.messages.s2c.service.RegisterServiceResult;

/**
 * 
 * @author Kasper Nielsen
 */
class ClientConnection extends AbstractClientConnection {

    final DefaultPersistentConnection cm;

    volatile String connectionId;

    ClientConnection(String id, DefaultPersistentConnection cn) {
        super(id, cn.cfs);
        this.cm = requireNonNull(cn);
        super.setTransport(new ClientTransport(cn));
    }

    /** {@inheritDoc} */
    public final void handleMessage(ConnectionMessage m) {
        if (m instanceof InvokeService) {
            invokeService((InvokeService) m);
        } else if (m instanceof InvokeServiceResult) {
            invokeServiceAck((InvokeServiceResult) m);
        } else if (m instanceof BroadcastMsg) {
            receivedBroadcast((BroadcastMsg) m);
        } else {
            unknownMessage(m);
        }
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
            unknownMessage(m);
        }
    }

    protected void unknownMessage(TransportMessage m) {
        System.err.println("Received an unknown message " + m.getReceivedRawMesage());
    }

    /** {@inheritDoc} */
    protected void invokeService(InvokeService m) {
        cm.services.receiveInvokeService(m);
    }

    /** {@inheritDoc} */
    protected void invokeServiceAck(InvokeServiceResult m) {
        cm.services.receiveInvokeServiceAck(m);
    }

    /** {@inheritDoc} */
    protected void receivedBroadcast(BroadcastMsg m) {
        cm.broadcaster.onBroadcastMessage(m);
    }

    /** {@inheritDoc} */
    protected void serviceFindAck(FindServiceResult a, DefaultConnectionFuture<FindServiceResult> f) {
        f.complete(a);
    }

    /** {@inheritDoc} */
    protected void serviceRegisteredAck(RegisterServiceResult a, DefaultConnectionFuture<RegisterServiceResult> f) {
        f.complete(a);
    }

    public void connect(long timeout, TimeUnit unit) throws IOException {
        try {
            cm.transportFactory.connect(getTransport(), timeout, unit);
            if (!((ClientTransport) getTransport()).awaitFullyConnected(timeout, unit)) {
                throw new ConnectException("Timedout while connecting to ");
            }
        } catch (IOException e) {
            cm.es.shutdown();
            cm.ses.shutdown();
            cm.transportFactory.shutdown();
            throw e;
        } catch (InterruptedException e) {
            throw new InterruptedIOException();
        }
    }
}
