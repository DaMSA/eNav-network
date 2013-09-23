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
package dk.dma.navnet.protocol;

import static java.util.Objects.requireNonNull;

import java.util.concurrent.locks.ReentrantLock;

import dk.dma.enav.communication.ClosingCode;
import dk.dma.navnet.messages.ConnectionMessage;

/**
 * 
 * @author Kasper Nielsen
 */
public abstract class Connection extends AbstractProtocol {

    private volatile Application application;

    /** The unique id of the connection. */
    private final String connectionId;

    public long latestLocalIdAcked;

    public long latestRemoteAckedIdSend;

    public long latestRemoteIdReceived;

    public final ReentrantLock lock = new ReentrantLock();

    ResumingQueue rq = new ResumingQueue();

    private volatile Transport transport;

    protected Connection(String id) {
        this.connectionId = requireNonNull(id);
    }

    public final void closeNormally() {
        transport.doClose(ClosingCode.NORMAL);
    }

    /**
     * @return the application
     */
    public Application getApplication() {
        return application;
    }

    public final String getConnectionId() {
        return connectionId;
    }

    public final Transport getTransport() {
        return transport;
    }

    public void onConnectionClose(ClosingCode reason) {}

    public void onConnectionCreate() {}

    public void onConnectionError(Throwable cause) {}

    public void onConnectionMessage(ConnectionMessage message) {
        latestRemoteIdReceived = message.getMessageId();
        rq.ackUpToIncluding(message.getLatestReceivedId());
    }

    public final void sendConnectionMessage(ConnectionMessage m) {
        m.setLatestReceivedId(latestRemoteIdReceived);
        rq.write(transport, m);
        // transport.sendTransportMessage(m);
    }

    /**
     * @param application
     *            the application to set
     */
    public void setApplication(Application application) {
        this.application = application;
        if (application == null) {
            // We should clear buffers here
            // will never work again
        }
    }

    public void setTransport(Transport transport) {
        lock.lock();
        try {
            Transport old = this.transport;
            if (old != null) {
                old.setConnection(null);
            }
            this.transport = transport;
            if (transport != null) {
                transport.setConnection(this);
            }
        } finally {
            lock.unlock();
        }
    }

    /** The current state of the transport. */
    public enum State {
        CLOSED, CONNECTED, CONNECTING, INITIALIZED;
    }
}
