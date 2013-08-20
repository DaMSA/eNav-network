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
package dk.dma.navnet.server;

import static java.util.Objects.requireNonNull;

import java.util.UUID;

import dk.dma.enav.model.MaritimeId;
import dk.dma.enav.model.geometry.PositionTime;
import dk.dma.navnet.core.messages.ConnectionMessage;
import dk.dma.navnet.core.messages.c2c.ClientRelayedMessage;
import dk.dma.navnet.core.messages.c2c.broadcast.BroadcastMsg;
import dk.dma.navnet.core.messages.s2c.service.FindService;
import dk.dma.navnet.core.messages.s2c.service.RegisterService;
import dk.dma.navnet.core.messages.transport.ConnectedMessage;
import dk.dma.navnet.core.messages.transport.HelloMessage;
import dk.dma.navnet.core.messages.transport.PositionReportMessage;
import dk.dma.navnet.protocol.connection.Connection;

/**
 * 
 * @author Kasper Nielsen
 */
public class ServerConnection extends Connection {

    final EmbeddableCloudServer server;

    final MaritimeId id;

    volatile Target target;
    /** The latest position of the client. */
    volatile PositionTime latestPosition;
    final ServerServiceManager services;
    final String remoteId;

    /**
     * @param cm
     * @param sh
     */
    public ServerConnection(EmbeddableCloudServer server, MaritimeId id, String connectionId) {
        super(connectionId);
        this.server = requireNonNull(server);
        this.remoteId = id.toString();
        this.id = requireNonNull(id);
        services = new ServerServiceManager(this);
    }

    /** {@inheritDoc} */
    @Override
    public final void onConnectionMessage(ConnectionMessage m) {
        if (m instanceof RegisterService) {
            registerService((RegisterService) m);
        } else if (m instanceof FindService) {
            services.findService((FindService) m);
        } else if (m instanceof ClientRelayedMessage) {
            relay((ClientRelayedMessage) m);
        } else if (m instanceof BroadcastMsg) {
            server.connectionManager.broadcast(this, (BroadcastMsg) m);
        } else if (m instanceof PositionReportMessage) {
            positionReport((PositionReportMessage) m);
        } else {
            // unknownMessage(m);
        }
    }

    /** {@inheritDoc} */
    public void positionReport(PositionReportMessage m) {
        Target target = this.target;
        if (target != null) {
            server.tracker.update(target, m.getPositionTime());
        }
        latestPosition = m.getPositionTime();
    }

    /** {@inheritDoc} */
    public void registerService(RegisterService m) {
        services.registerService(m);
        sendConnectionMessage(m.createReply());
    }

    /** {@inheritDoc} */
    public void relay(ClientRelayedMessage m) {
        String d = m.getDestination();
        ServerConnection c = server.connectionManager.getConnection(d);
        if (c == null) {
            System.err.println("Unknown destination " + d);
            // System.err.println("Available " + server.connectionManager.getAllConnectionIds());
        } else {
            c.sendConnectionMessage(m.cloneIt());
            // c.sendRawTextMessage(m.getReceivedRawMesage());
        }
    }

    static ServerConnection connect(EmbeddableCloudServer server, Target target, ServerTransport connectingTransport,
            ServerConnection existing, HelloMessage message) {
        String reconnectId = message.getReconnectId();

        final PositionTime pt = new PositionTime(message.getLat(), message.getLon(), -1);
        if (existing == null || reconnectId.equals("")) {
            ServerConnection c = new ServerConnection(server, message.getClientId(), UUID.randomUUID().toString());
            c.latestPosition = pt;
            c.setTransport(connectingTransport);
            connectingTransport.sendTransportMessage(new ConnectedMessage(c.getConnectionId()));
            server.tracker.update(target, pt);
            return c;
        }
        throw new Error();
    }
}
