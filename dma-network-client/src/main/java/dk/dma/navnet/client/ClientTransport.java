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

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import dk.dma.enav.communication.ClosingCode;
import dk.dma.enav.model.geometry.PositionTime;
import dk.dma.navnet.messages.TransportMessage;
import dk.dma.navnet.messages.auxiliary.ConnectedMessage;
import dk.dma.navnet.messages.auxiliary.HelloMessage;
import dk.dma.navnet.messages.auxiliary.WelcomeMessage;
import dk.dma.navnet.protocol.transport.Transport;

/**
 * The client implementation of a transport
 * 
 * @author Kasper Nielsen
 */
class ClientTransport extends Transport {

    private final ClientState client;

    /** A latch that is released when the client receives a ConnectedMessage from the server. */
    private final CountDownLatch fullyConnected = new CountDownLatch(1);

    private final long reconnectId;

    ClientTransport(ClientState client) {
        this(client, -1);
    }

    ClientTransport(ClientState client, long reconnectId) {
        this.client = requireNonNull(client);
        this.reconnectId = reconnectId;
    }

    boolean awaitFullyConnected(long timeout, TimeUnit unit) throws InterruptedException {
        return fullyConnected.await(timeout, unit);
    }

    /** {@inheritDoc} */
    @Override
    public void onTransportClose(ClosingCode reason) {
        ClientConnection cc = (ClientConnection) getConnection();
        if (cc != null) {
            cc.cm.close();
        }
    }

    /** {@inheritDoc} */
    @Override
    public void onTransportMessage(TransportMessage message) {
        if (message instanceof WelcomeMessage) {
            // WelcomeMessage m = (WelcomeMessage) message; we do not care about the contents atm
            PositionTime pt = client.getCurrentPosition();
            sendTransportMessage(new HelloMessage(client.getLocalId(), "enavClient/1.0", "", reconnectId,
                    pt.getLatitude(), pt.getLongitude()));
        } else if (message instanceof ConnectedMessage) {
            ConnectedMessage m = (ConnectedMessage) message;
            ((ClientConnection) getConnection()).connectionId = m.getConnectionId();
            fullyConnected.countDown();
        } else {
            super.onTransportMessage(message);
        }
    }
}
