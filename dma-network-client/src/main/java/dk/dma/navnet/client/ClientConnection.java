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
package dk.dma.navnet.client;

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.net.ConnectException;
import java.net.URI;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.eclipse.jetty.websocket.client.WebSocketClient;

import com.fasterxml.jackson.databind.ObjectMapper;

import dk.dma.enav.net.ServiceCallback;
import dk.dma.enav.net.broadcast.BroadcastMessage;
import dk.dma.enav.net.broadcast.BroadcastProperties;
import dk.dma.navnet.client.ClientNetwork.BSubcription;
import dk.dma.navnet.core.messages.c2c.Broadcast;
import dk.dma.navnet.core.messages.c2c.InvokeService;
import dk.dma.navnet.core.messages.s2c.connection.ConnectedMessage;
import dk.dma.navnet.core.messages.s2c.connection.HelloMessage;
import dk.dma.navnet.core.messages.s2c.connection.WelcomeMessage;
import dk.dma.navnet.core.spi.ClientHandler;

/**
 * 
 * @author Kasper Nielsen
 */
class ClientConnection extends ClientHandler {

    final WebSocketClient client = new WebSocketClient();

    final ClientNetwork cm;
    final CountDownLatch connected = new CountDownLatch(1);

    long nextReplyId;

    State state = State.CREATED;

    private final String url;

    ClientConnection(String url, ClientNetwork cm) {
        this.cm = requireNonNull(cm);
        this.url = requireNonNull(url);
    }

    /** {@inheritDoc} */
    @Override
    protected void receivedBroadcast(Broadcast m) {
        BroadcastMessage bm = null;
        Class<?> cl = null;
        try {
            String channel = m.getChannel();
            ObjectMapper om = new ObjectMapper();
            cl = Class.forName(channel);
            bm = (BroadcastMessage) om.readValue(m.getMessage(), cl);
        } catch (Exception e) {
            e.printStackTrace();
        }
        // Okay vi have a valid broadcast message
        if (bm != null) {
            CopyOnWriteArraySet<BSubcription> s = cm.subscribers.get(cl);
            if (s != null) {
                BroadcastProperties bp = new BroadcastProperties(m.getId(), m.getPositionTime());
                for (BSubcription c : s) {
                    c.deliver(bp, bm);
                }
            }
        }
    }

    public void close() throws IOException {
        tryClose(4333, "Goodbye");
        try {
            client.stop();
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    public void connect() throws Exception {
        URI echoUri = new URI(url);
        client.start();
        try {
            client.connect(getListener(), echoUri).get();
            connected.await(10, TimeUnit.SECONDS);
            if (connected.getCount() > 0) {
                throw new ConnectException("Timedout while connecting to " + url);
            }
        } catch (ExecutionException e) {
            cm.es.shutdown();
            cm.ses.shutdown();
            client.stop();
            throw (Exception) e.getCause();// todo fix throw
        }
    }

    /** {@inheritDoc} */
    @Override
    public void connected(ConnectedMessage m) {
        connected.countDown();
    }

    /** {@inheritDoc} */
    @Override
    public void invokeService(InvokeService m) {
        InternalServiceCallbackRegistration s = cm.registeredServices.get(m.getServiceType());
        if (s != null) {
            ServiceCallback<Object, Object> sc = s.c;
            Object o = null;
            try {
                Class<?> mt = Class.forName(s.type.getName() + "$" + m.getServiceMessageType());
                ObjectMapper om = new ObjectMapper();
                o = om.readValue(m.getMessage(), mt);
            } catch (Exception e) {
                e.printStackTrace();
            }
            sc.process(o, new ServiceCallback.Context<Object>() {
                public void complete(Object result) {
                    requireNonNull(result);
                    System.out.println("Completed");
                    // con.packetWrite(p.replyWith(result));
                }

                public void fail(Throwable cause) {
                    requireNonNull(cause);
                    System.out.println(cause);
                    // con.packetWrite(p.replyWithFailure(cause));
                }
            });
        }
        super.invokeService(m);
    }

    /** {@inheritDoc} */
    @Override
    public void welcome(WelcomeMessage m) {
        sendMessage(new HelloMessage(cm.clientId, "enavClient/1.0", "", 2));
    }

    enum State {
        CONNECTED, CREATED, DISCONNECTED
    }
}
