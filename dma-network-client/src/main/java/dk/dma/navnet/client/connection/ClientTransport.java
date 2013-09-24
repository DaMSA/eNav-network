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

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.websocket.ClientEndpoint;

import dk.dma.enav.communication.ClosingCode;
import dk.dma.enav.model.geometry.PositionTime;
import dk.dma.enav.util.function.Supplier;
import dk.dma.navnet.client.ClientInfo;
import dk.dma.navnet.messages.ConnectionMessage;
import dk.dma.navnet.messages.TransportMessage;
import dk.dma.navnet.messages.auxiliary.ConnectedMessage;
import dk.dma.navnet.messages.auxiliary.HelloMessage;
import dk.dma.navnet.messages.auxiliary.WelcomeMessage;
import dk.dma.navnet.protocol.Transport;

/**
 * The client implementation of a transport
 * 
 * @author Kasper Nielsen
 */
@ClientEndpoint
public class ClientTransport extends Transport {

    private final ClientInfo clientInfo;

    /** A latch that is released when the client receives a ConnectedMessage from the server. */
    private final CountDownLatch fullyConnected = new CountDownLatch(1);

    private final long reconnectId;

    private final Supplier<PositionTime> positionSupplier;

    ClientTransport(ClientInfo clientInfo, Supplier<PositionTime> positionSupplier) {
        this(clientInfo, positionSupplier, -1);
    }

    ClientTransport(ClientInfo clientInfo, Supplier<PositionTime> positionSupplier, long reconnectId) {
        this.clientInfo = requireNonNull(clientInfo);
        this.reconnectId = reconnectId;
        this.positionSupplier = positionSupplier;
    }

    boolean awaitFullyConnected(long timeout, TimeUnit unit) throws InterruptedException {
        return fullyConnected.await(timeout, unit);
    }

    /** {@inheritDoc} */
    @Override
    public void onTransportClose(ClosingCode reason) {
        System.out.println("CLOSED XXXXXXXXXXXX " + reason.getId());
        ClientConnection cc = (ClientConnection) getConnection();
        if (cc != null) {
            cc.disconnectOops(reason);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void onTransportMessage(TransportMessage message) {
        if (message instanceof WelcomeMessage) {
            // WelcomeMessage m = (WelcomeMessage) message; we do not care about the contents atm
            PositionTime pt = positionSupplier.get();
            doSendTransportMessage(new HelloMessage(clientInfo.getLocalId(), "enavClient/1.0", "", reconnectId,
                    pt.getLatitude(), pt.getLongitude()));
        } else if (message instanceof ConnectedMessage) {
            ConnectedMessage m = (ConnectedMessage) message;
            ((ClientConnection) getConnection()).connectionId = m.getConnectionId();
            fullyConnected.countDown();
        } else {
            ConnectionMessage cm = (ConnectionMessage) message;
            getConnection().onConnectionMessage(cm);
            // super.onTransportMessage(message);
        }
    }
}
