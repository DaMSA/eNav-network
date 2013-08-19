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
package dk.dma.navnet.protocol.connection;

import static java.util.Objects.requireNonNull;

import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.locks.ReentrantLock;

import dk.dma.enav.communication.CloseReason;
import dk.dma.navnet.core.messages.ConnectionMessage;
import dk.dma.navnet.core.messages.TransportMessage;
import dk.dma.navnet.protocol.AbstractProtocol;
import dk.dma.navnet.protocol.transport.Transport;

/**
 * 
 * @author Kasper Nielsen
 */
public abstract class Connection extends AbstractProtocol {

    /** The unique id of the connection. */
    private final String connectionId;

    public long latestLocalIdAcked;

    public long latestLocalIdSend;
    public long latestRemoteAckedIdSend;

    public long latestRemoteIdReceived;

    public final ReentrantLock lock = new ReentrantLock();

    /** All messages that not yet been acked by the remote end. */
    public final ConcurrentSkipListMap<Long, TransportMessage> nonAcked = new ConcurrentSkipListMap<>();

    private volatile Transport transport;

    protected Connection(String id) {
        this.connectionId = requireNonNull(id);
    }

    public final void closeNormally() {
        transport.close(CloseReason.NORMAL);
    }

    public final String getConnectionId() {
        return connectionId;
    }

    public final Transport getTransport() {
        return transport;
    }

    public void onConnectionClose(CloseReason reason) {}

    public void onConnectionCreate() {}

    public void onConnectionError(Throwable cause) {}

    public void onConnectionMessage(ConnectionMessage message) {}

    public final void sendConnectionMessage(ConnectionMessage m) {
        transport.sendTransportMessage(m);
    }

    public void setTransport(Transport transport) {
        lock.lock();
        try {
            Transport old = this.transport;
            if (old != null) {
                old.setConnection(null);
            }
            this.transport = transport;
            transport.setConnection(this);
        } finally {
            lock.unlock();
        }
    }
}
