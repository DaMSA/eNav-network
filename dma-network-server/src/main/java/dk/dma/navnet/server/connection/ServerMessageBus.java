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

import java.util.concurrent.CopyOnWriteArraySet;

import jsr166e.ConcurrentHashMapV8;
import dk.dma.enav.util.function.BiConsumer;
import dk.dma.navnet.messages.ConnectionMessage;
import dk.dma.navnet.messages.s2c.ServerRequestMessage;
import dk.dma.navnet.messages.s2c.ServerResponseMessage;
import dk.dma.navnet.messages.util.ResumingClientQueue.OutstandingMessage;

/**
 * 
 * @author Kasper Nielsen
 */
public class ServerMessageBus {
    /** Consumers of messages. */
    final CopyOnWriteArraySet<MessageConsumer> consumers = new CopyOnWriteArraySet<>();

    final ConcurrentHashMapV8<Class<?>, RequestProcessor> requests = new ConcurrentHashMapV8<>();

    void onMessage(ServerConnection connection, ConnectionMessage message) {
        boolean handled = false;
        try {
            System.out.println("XXXX " + message.getClass());
            System.out.println(requests.keySet());
            RequestProcessor rp = requests.get(message.getClass());
            if (rp != null) {

                ServerResponseMessage srm = rp.process(connection, (ServerRequestMessage) message);
                connection.messageSend(srm);
                return;
            }

            for (MessageConsumer c : consumers) {
                if (c.type.isAssignableFrom(message.getClass())) {
                    handled = true;
                    c.c.accept(connection, message);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (!handled) {
            System.out.println("Message was not handled: " + message.toJSON());
        }
    }

    public OutstandingMessage sendConnectionMessage(ServerConnection connection, ConnectionMessage b) {
        return connection.messageSend(b);
    }

    @SuppressWarnings("unchecked")
    public <T extends ConnectionMessage> void subscribe(Class<T> type, BiConsumer<ServerConnection, ? super T> c) {
        consumers.add(new MessageConsumer(type, (BiConsumer<ServerConnection, ConnectionMessage>) c));
    }

    public <S extends ServerRequestMessage<T>, T extends ServerResponseMessage> void subscribe(Class<S> type,
            RequestProcessor<S, T> p) {
        requests.putIfAbsent(type, p);
    }

    static class MessageConsumer {

        final BiConsumer<ServerConnection, ConnectionMessage> c;

        final Class<?> type;

        MessageConsumer(Class<?> type, BiConsumer<ServerConnection, ConnectionMessage> c) {
            this.type = requireNonNull(type);
            this.c = requireNonNull(c);
        }
    }
}
