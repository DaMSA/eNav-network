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
import dk.dma.enav.model.MaritimeId;
import dk.dma.enav.model.geometry.PositionTime;
import dk.dma.navnet.core.messages.transport.ConnectedMessage;

/**
 * 
 * @author Kasper Nielsen
 */
abstract class ClientState {

    /** The id of this client */
    private final MaritimeId clientId;

    ClientState(MaritimeNetworkConnectionBuilder builder) {
        this.clientId = requireNonNull(builder.getId());
    }

    /** The single connection to a server. */
    volatile ClientConnection connection;

    public final MaritimeId getLocalId() {
        return clientId;
    }

    abstract PositionTime getCurrentPosition();

    void transportConnected(ClientTransport transport, ConnectedMessage message) {
        String connectionId = message.getConnectionId();
        ((ClientConnection) transport.getConnection()).connectionId = connectionId;

    }
}
