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
package dk.dma.navnet.server.requests;

import static java.util.Objects.requireNonNull;
import jsr166e.ConcurrentHashMapV8;
import dk.dma.enav.model.MaritimeId;
import dk.dma.navnet.messages.ConnectionMessage;
import dk.dma.navnet.messages.PositionTimeMessage;
import dk.dma.navnet.messages.c2c.ClientRelayedMessage;
import dk.dma.navnet.messages.s2c.ServerRequestMessage;
import dk.dma.navnet.messages.s2c.ServerResponseMessage;
import dk.dma.navnet.server.connection.ServerConnection;
import dk.dma.navnet.server.target.Target;
import dk.dma.navnet.server.target.TargetManager;

/**
 * 
 * @author Kasper Nielsen
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class ServerMessageBus {

    private final ConcurrentHashMapV8<Class<?>, RequestProcessor> processors = new ConcurrentHashMapV8<>();

    final TargetManager tm;

    public ServerMessageBus(TargetManager tm) {
        this.tm = requireNonNull(tm);
    }

    public void onMessage(ServerConnection connection, ConnectionMessage message) {
        if (message instanceof PositionTimeMessage) {
            connection.getTarget().setLatestPosition(((PositionTimeMessage) message).getPositionTime());
        }
        if (message instanceof ServerRequestMessage) {
            onServerRequestMessage(connection, (ServerRequestMessage) message);
        } else if (message instanceof ClientRelayedMessage) {
            relay((ClientRelayedMessage) message);
        }
    }


    public void relay(ClientRelayedMessage m) {
        String d = m.getDestination();
        Target t = tm.find(MaritimeId.create(d));
        if (t == null) {
            System.err.println("Unknown destination " + d);
            return;
        }
        ServerConnection sc = t.getConnection();
        if (sc == null) {
            System.err.println("Unknown destination " + d);
            return;
        }
        System.out.println("RELAY");
        sc.messageSend(m.cloneIt());
    }


    public void onServerRequestMessage(ServerConnection connection, ServerRequestMessage message) {
        RequestProcessor rp = processors.get(message.getClass());
        if (rp == null) {
            System.out.println("No processors for: " + message.getClass() + "[" + message.toJSON() + "]");
        }

        try {
            ServerResponseMessage srm = rp.process(connection, message);
            connection.messageSend(srm);
            return;
        } catch (RequestException e) {
            // send error message
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public <S extends ServerRequestMessage<T>, T extends ServerResponseMessage> void subscribe(Class<S> type,
            RequestProcessor<S, T> p) {
        processors.putIfAbsent(type, p);
    }
}
