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

import java.util.concurrent.locks.ReentrantLock;

import jsr166e.ForkJoinPool;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.dma.enav.communication.ClosingCode;
import dk.dma.navnet.messages.ConnectionMessage;
import dk.dma.navnet.messages.util.ResumingClientQueue;
import dk.dma.navnet.messages.util.ResumingClientQueue.OutstandingMessage;

/**
 * 
 * @author Kasper Nielsen
 */
public class ClientConnection {

    /** The logger. */
    static final Logger LOG = LoggerFactory.getLogger(ClientConnection.class);

    volatile String connectionId;

    final ConnectionManager connectionManager;

    final ReentrantLock retrieveLock = new ReentrantLock();

    final ReentrantLock sendLock = new ReentrantLock();

    final ResumingClientQueue rq = new ResumingClientQueue();

    /* State managed objects */
    private volatile ClientTransport transport;

    /**
     * @return the transport
     */
    public ClientTransport getTransport() {
        return transport;
    }

    private volatile ClientConnectFuture connectingFuture;

    private volatile ClientDisconnectFuture disconnectingFuture;

    public ClientConnection(ConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
    }

    boolean isConnected() {
        connectionManager.lock.lock();
        try {
            return transport != null && disconnectingFuture == null;
        } finally {
            connectionManager.lock.unlock();
        }
    }

    void connect() {
        connectionManager.lock.lock();
        try {
            if (transport == null && connectingFuture == null) {
                LOG.info("Trying to connect");
                connectingFuture = new ClientConnectFuture(this, -1);
                ForkJoinPool.commonPool().submit(connectingFuture);
            }
        } finally {
            connectionManager.lock.unlock();
        }
    }

    /**
     * Invoked when we have successfully connected to the server.
     * 
     * @param transport
     */
    void connected(ClientConnectFuture future, ClientTransport transport) {
        connectionManager.lock.lock();
        try {
            if (future == connectingFuture) {
                this.connectingFuture = null;
                this.transport = transport;
                connectionManager.stateChange.signalAll();
            }
        } finally {
            connectionManager.lock.unlock();
        }
    }

    void disconnect() {
        connectionManager.lock.lock();
        try {
            if (transport != null) {
                LOG.info("Trying to disconnect");
                disconnectingFuture = new ClientDisconnectFuture(this, transport);
                ForkJoinPool.commonPool().submit(disconnectingFuture);
            } else if (connectingFuture != null) {
                LOG.info("Trying to disconnect");
                // We are in the process of connecting, just cancel the connect
                connectingFuture.cancelConnectUnderLock();
            }
            connectionManager.stateChange.signalAll();
        } finally {
            connectionManager.lock.unlock();
        }
    }

    void disconnected(ClientDisconnectFuture future) {
        connectionManager.lock.lock();
        try {
            if (future == disconnectingFuture && connectingFuture == null && transport == null) {
                this.disconnectingFuture = null;
            }
        } finally {
            connectionManager.lock.unlock();
        }
    }

    /**
     * Invoked whenever we want to send a message
     * 
     * @param message
     *            the message to send
     */
    ResumingClientQueue.OutstandingMessage messageSend(ConnectionMessage message) {
        sendLock.lock();
        try {

            OutstandingMessage m = rq.write(message);
            if (transport != null) {
                System.out.println("Sending " + m.msg);
                transport.sendText(m.msg);
            } else {
                System.out.println("Not sending " + m.msg);
            }

            return m;
        } finally {
            sendLock.unlock();
        }
    }

    void messageReceive(ClientTransport transport, ConnectionMessage m) {
        retrieveLock.lock();
        try {
            rq.messageIn(m);
            connectionManager.hub.onMsg(m);
        } finally {
            retrieveLock.unlock();
        }
    }

    void transportDisconnected(ClientTransport transport, ClosingCode cr) {
        connectionManager.lock.lock();
        try {
            if (cr.getId() == 1000) {
                connectionManager.connection = null;
            } else if (cr.getId() == ClosingCode.DUPLICATE_CONNECT.getId()) {
                System.out.println("Dublicate connect detected, will not reconnect");
                connectionManager.state = ConnectionManager.State.SHOULD_STAY_DISCONNECTED;
                this.transport = null;
            } else {
                System.out.println(cr.getMessage());
                System.out.println("OOPS, lets reconnect");
            }
            connectionManager.stateChange.signalAll();
        } finally {
            connectionManager.lock.unlock();
        }

        // if (cr.getId() == 1000) {
        // connectionManager.connection = null;
        //
        //
        // } else {
        //
        // connectionManager.lock.lock();
        // try {
        //
        // if ()
        // } finally {
        // connectionManager.lock.unlock();
        // }
        // }
    }
}
