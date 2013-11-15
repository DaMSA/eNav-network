/*
 * Copyright (c) 2008 Kasper Nielsen.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package dk.dma.navnet.server.connection;

import static java.util.Objects.requireNonNull;

import java.util.UUID;

import dk.dma.enav.communication.ClosingCode;
import dk.dma.enav.model.MaritimeId;
import dk.dma.navnet.messages.ConnectionMessage;
import dk.dma.navnet.messages.PositionTimeMessage;
import dk.dma.navnet.messages.c2c.ClientRelayedMessage;
import dk.dma.navnet.messages.util.ResumingClientQueue;
import dk.dma.navnet.messages.util.ResumingClientQueue.OutstandingMessage;
import dk.dma.navnet.server.InternalServer;
import dk.dma.navnet.server.target.Target;
import dk.dma.navnet.server.target.TargetManager;

/**
 * 
 * @author Kasper Nielsen
 */
public class ServerConnection {

    private final ServerMessageBus bus;

    final String id = UUID.randomUUID().toString();

    final ResumingClientQueue rq = new ResumingClientQueue();

    final Target target;

    volatile ServerTransport transport;

    final InternalServer is;

    ServerConnection(Target target, InternalServer is) {
        this.target = requireNonNull(target);
        this.bus = requireNonNull(is.getService(ServerMessageBus.class));
        this.is = is;
    }

    /**
     * @return the id
     */
    public String getConnectionId() {
        return id;
    }

    /**
     * @return the target
     */
    public Target getTarget() {
        return target;
    }

    /**
     * @param serverTransport
     * @param m
     */
    public void messageReceive(ServerTransport serverTransport, ConnectionMessage m) {
        if (m instanceof PositionTimeMessage) {
            target.setLatestPosition(((PositionTimeMessage) m).getPositionTime());
        } else if (m instanceof ClientRelayedMessage) {
            relay((ClientRelayedMessage) m);
            return;
        }
        rq.messageIn(m);
        bus.onMessage(this, m);
    }

    public void relay(ClientRelayedMessage m) {
        String d = m.getDestination();
        Target t = is.getService(TargetManager.class).find(MaritimeId.create(d));
        if (t == null) {
            System.err.println("Unknown destination " + d);
            return;
        }
        ServerConnection sc = t.getConnection();
        if (sc == null) {
            System.err.println("Unknown destination " + d);
            return;
        }

        sc.messageSend(m.cloneIt());

        // ServerConnection c = transport.server.connectionManager.getConnection(d);
        // if (c == null) {
        // System.err.println("Unknown destination " + d);
        // // System.err.println("Available " + server.connectionManager.getAllConnectionIds());
        // } else {
        // c.sendConnectionMessage(m.cloneIt());
        // // c.sendRawTextMessage(m.getReceivedRawMesage());
        // }
    }


    public ResumingClientQueue.OutstandingMessage messageSend(ConnectionMessage message) {
        // sendLock.lock();
        // try {
        OutstandingMessage m = rq.write(message);
        if (transport != null) {
            transport.sendText(message.toJSON());
        }
        return m;
        // return m;
        // } finally {
        // sendLock.unlock();
        // }
    }

    /**
     * @param serverTransport
     * @param reason
     */
    public void transportDisconnected(ServerTransport serverTransport, ClosingCode reason) {}
}
