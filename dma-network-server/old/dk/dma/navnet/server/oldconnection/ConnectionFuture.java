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
package dk.dma.navnet.server.oldconnection;

import static java.util.Objects.requireNonNull;
import dk.dma.enav.communication.ClosingCode;
import dk.dma.enav.model.geometry.PositionTime;
import dk.dma.navnet.messages.auxiliary.HelloMessage;


/**
 * 
 * @author Kasper Nielsen
 */
class ConnectionFuture {

    final ServerTransport transport;

    final ConnectionManager cm;

    volatile HelloMessage hm;

    ConnectionFuture(ConnectionManager cm, ServerTransport transport) {
        this.transport = requireNonNull(transport);
        this.cm = cm;
    }

    void helloSend() {
        cm.connectingTransports.add(this);
    }

    void finished(HelloMessage hm) {
        this.hm = hm;
    }

    void connected(Target target, HelloMessage m) {
        target.lock();
        try {
            ServerConnection connection = target.currentConnection;
            if (connection == null) {
                connectedNoConnection(target);
            } else {
                connectedConnection(target, connection);
            }
        } finally {
            target.unlock();
        }
    }

    void connectedConnection(Target target, ServerConnection existing) {
        existing.fullyLock();
        try {
            ServerTransport existingTransport = existing.transport;
            if (existingTransport != null) {
                existing.transport = null;
                existingTransport.connection = null;
                existingTransport.doClose(ClosingCode.DUPLICATE_CONNECT);
            }


        } finally {
            existing.fullyUnlock();
        }
    }

    void connectedNoConnection(Target target) {
        String reconnectId = hm.getReconnectId();
        // trying to reconnect but we cannot because we have no knowledge about the connection
        if (reconnectId.length() > 0) {
            // transport.close
        }
        final PositionTime pt = new PositionTime(hm.getLat(), hm.getLon(), -1);
        if (existing == null || reconnectId.equals("")) {


        }


    }
}
