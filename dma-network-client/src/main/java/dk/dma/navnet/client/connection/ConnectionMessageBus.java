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

import static java.util.Objects.requireNonNull;

import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicInteger;

import org.picocontainer.PicoContainer;
import org.picocontainer.Startable;

import dk.dma.navnet.client.util.DefaultConnectionFuture;
import dk.dma.navnet.client.util.ThreadManager;
import dk.dma.navnet.messages.ConnectionMessage;
import dk.dma.navnet.messages.s2c.ServerRequestMessage;
import dk.dma.navnet.messages.s2c.ServerResponseMessage;
import dk.dma.navnet.messages.s2c.service.FindServiceResult;
import dk.dma.navnet.messages.s2c.service.RegisterServiceResult;

/**
 * 
 * @author Kasper Nielsen
 */
public class ConnectionMessageBus implements Startable {

    /** Consumers of messages. */
    final CopyOnWriteArraySet<MessageConsumer> consumers = new CopyOnWriteArraySet<>();

    /** The connection manager. */
    final ConnectionManager cm;

    /** The thread manager. */
    final ThreadManager threadManager;

    final ConcurrentHashMap<Long, DefaultConnectionFuture<?>> acks = new ConcurrentHashMap<>();

    final AtomicInteger ai = new AtomicInteger();

    final PicoContainer container;

    public ConnectionMessageBus(PicoContainer container, ConnectionManager cm, ThreadManager threadManager) {
        this.cm = cm;
        this.container = container;

        cm.hub = this;
        this.threadManager = threadManager;
    }

    private ClientConnection connection() {
        ClientConnection connection = cm.connection;
        if (connection == null) {
            throw new IllegalStateException("Client has not been connected yet, or is running in disconnect mode");
        }
        return connection;
    }

    /** {@inheritDoc} */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    private final void handleMessageReply(ConnectionMessage m, DefaultConnectionFuture<?> f) {
        if (m instanceof RegisterServiceResult) {
            serviceRegisteredAck((RegisterServiceResult) m, (DefaultConnectionFuture<RegisterServiceResult>) f);
        } else if (m instanceof FindServiceResult) {
            serviceFindAck((FindServiceResult) m, (DefaultConnectionFuture<FindServiceResult>) f);
        } else {
            ((DefaultConnectionFuture) f).complete(m);
        }
    }

    public void onMsg(ConnectionMessage m) {
        if (m instanceof ServerResponseMessage) {
            ServerResponseMessage am = (ServerResponseMessage) m;
            DefaultConnectionFuture<?> f = acks.remove(am.getMessageAck());
            if (f == null) {
                System.err.println("Orphaned packet with id " + am.getMessageAck() + " registered " + acks.keySet()
                        + ", local " + "" + " p = ");
                // TODO close connection with error
            } else {
                try {
                    handleMessageReply(m, f);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            // System.out.println("RELEASING " + am.getMessageAck() + ", remaining " + acks.keySet());
        } else {
            try {
                for (MessageConsumer c : consumers) {
                    if (c.type.isAssignableFrom(m.getClass())) {
                        c.process(m);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

    public OutstandingMessage sendConnectionMessage(ConnectionMessage b) {
        return connection().messageSend(b);
    }

    public <T extends ServerResponseMessage> DefaultConnectionFuture<T> sendMessage(ServerRequestMessage<T> m) {
        // we need to send the messages in the same order as they are numbered for now
        synchronized (ai) {
            long id = ai.incrementAndGet();

            DefaultConnectionFuture<T> f = threadManager.create();
            acks.put(id, f);
            m.setReplyTo(id);
            sendConnectionMessage(m);
            return f;
        }
    }

    /** {@inheritDoc} */
    private void serviceFindAck(FindServiceResult a, DefaultConnectionFuture<FindServiceResult> f) {
        f.complete(a);
    }

    /** {@inheritDoc} */
    private void serviceRegisteredAck(RegisterServiceResult a, DefaultConnectionFuture<RegisterServiceResult> f) {
        f.complete(a);
    }

    static class MessageConsumer {

        final Method m;

        final Class<?> type;

        final Object o;

        MessageConsumer(Class<?> type, Object o, Method m) {
            this.type = requireNonNull(type);
            this.o = requireNonNull(o);
            this.m = m;
        }

        void process(Object message) {
            try {
                m.invoke(o, message);
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException(e);
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public void start() {
        for (Object o : container.getComponents()) {
            for (Method m : o.getClass().getMethods()) {
                if (m.isAnnotationPresent(OnMessage.class)) {
                    @SuppressWarnings("rawtypes")
                    Class messageType = m.getParameterTypes()[0];
                    consumers.add(new MessageConsumer(messageType, o, m));
                }
            }
        }
    }


    /** {@inheritDoc} */
    @Override
    public void stop() {}
}
