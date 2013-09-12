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
package dk.dma.navnet.protocol.transport;

import static java.util.Objects.requireNonNull;
import dk.dma.enav.communication.ClosingCode;
import dk.dma.navnet.messages.ConnectionMessage;
import dk.dma.navnet.messages.TransportMessage;
import dk.dma.navnet.protocol.AbstractProtocol;
import dk.dma.navnet.protocol.connection.Connection;

/**
 * The main purpose of the transport layer is to convert web socket messages to {@link TransportMessage} and the other
 * way. If a transport accidently break. The connection layer will automatically create a new one, and re send any
 * packets that might not have been received by the remote end.
 * 
 * @author Kasper Nielsen
 */
public abstract class Transport extends AbstractProtocol {

    /** If closed, the reason why this transport was closed. */
    private volatile ClosingCode closeReason;

    private volatile Connection connection;

    /** The websocket listener, or null if not yet connected. */
    private volatile TransportListener session;

    /** The current state of the transport */
    private volatile State state = State.INITIALIZED;

    public final void close(ClosingCode reason) {
        requireNonNull(reason);
        fullyLock();
        try {
            if (state == State.CLOSED) {
                return;
            }
            closeReason = reason;
            if (session != null) {
                session.close(reason);
            }
        } finally {
            fullyUnlock();
        }
    }

    final void closedByWebsocket(ClosingCode reason) {
        fullyLock();
        try {
            onTransportClose(reason);
        } finally {
            fullyUnlock();
        }
    }

    /**
     * If this transport has been closed. The reason for closing it. Returns <code>null</code> if the transport is still
     * active.
     */
    public final ClosingCode getCloseReason() {
        return closeReason;
    }

    /**
     * @return the connection
     */
    public final Connection getConnection() {
        return connection;
    }

    /**
     * Returns the current state of the transport.
     * 
     * @return the current state of the transport
     */
    public final State getState() {
        return state;
    }

    public void onTransportClose(ClosingCode reason) {}

    /**
     * A remote end has connected successfully and the transport is ready to be used.
     */
    public void onTransportConnect() {}

    public void onTransportError(Throwable cause) {
        cause.printStackTrace();
        System.out.println("ERROR " + cause);
    }

    /**
     * A message was received from the remote end point
     * 
     * @param message
     *            the message that was received
     */
    public void onTransportMessage(TransportMessage message) {
        connection.onConnectionMessage((ConnectionMessage) message);
    }

    /**
     * Receives a string message. The default implementation does nothing.
     * 
     * @param message
     *            the string message
     */
    void rawReceive(String message) {
        System.out.println("Received: " + message);
        try {
            onTransportMessage(TransportMessage.parseMessage(message));
        } catch (Throwable e) {
            e.printStackTrace();
            close(ClosingCode.WRONG_MESSAGE.withMessage(e.getMessage()));
        }
    }

    /**
     * Asynchronous sends a text to the remote end.
     * 
     * @param text
     *            the text to send
     * @throws IllegalStateException
     *             if the transport is not yet connected
     * @throws NullPointerException
     *             if the specified text is null
     */
    final void rawSend(String text) {
        requireNonNull(text, "text is null");
        TransportListener session = this.session;
        if (session == null) {
            throw new IllegalStateException("Not connected yet");
        }
        session.sendText(text);
    }

    public final void sendTransportMessage(TransportMessage m) {
        String msg = m.toJSON();
        try {
            System.out.println("Sending " + msg);
            rawSend(msg);
        } catch (Exception e) {
            onTransportError(e);
            e.printStackTrace();
        }
    }

    /**
     * @param connection
     *            the connection to set
     */
    public final void setConnection(Connection connection) {
        fullyLock();
        try {
            this.connection = connection;
        } finally {
            fullyUnlock();
        }
    }

    final void setSession(TransportListener session) {
        fullyLock();
        try {
            this.session = requireNonNull(session, "session is null");
            onTransportConnect();
        } finally {
            fullyUnlock();
        }
    }

    /** The current state of the transport. */
    public enum State {
        CLOSED, CONNECTED, CONNECTING, INITIALIZED;
    }
}
