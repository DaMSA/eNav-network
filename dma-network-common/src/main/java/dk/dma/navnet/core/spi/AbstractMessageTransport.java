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
package dk.dma.navnet.core.spi;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.locks.ReentrantLock;

import dk.dma.navnet.core.messages.AbstractTextMessage;
import dk.dma.navnet.core.messages.c2c.AbstractRelayedMessage;
import dk.dma.navnet.core.messages.s2c.AckMessage;
import dk.dma.navnet.core.messages.s2c.ReplyMessage;
import dk.dma.navnet.core.transport.Transport;
import dk.dma.navnet.core.transport.TransportSession;
import dk.dma.navnet.core.util.NetworkFutureImpl;

/**
 * 
 * @author Kasper Nielsen
 */
public abstract class AbstractMessageTransport extends Transport {

    protected AbstractConnection ac;

    public final CountDownLatch connected = new CountDownLatch(1);

    protected final ReentrantLock lock = new ReentrantLock();

    ScheduledExecutorService ses;

    protected final AbstractConnection client() {
        return ac;
    }

    protected void closed(int statusCode, String reason) {

    }

    public void connected() {}

    protected void failed(String message) {

    }

    /** {@inheritDoc} */
    @Override
    public final void onClosed(int code, String message) {
        super.onClosed(code, message);
        closed(code, message);
        System.out.println("CLOSED:" + message);
    }

    /** {@inheritDoc} */
    @Override
    public void onConnected(TransportSession spi) {
        super.onConnected(spi);
        connected();
    }

    public void onError(Throwable cause) {
        cause.printStackTrace();
        System.out.println("ERROR " + cause);
    }

    /** {@inheritDoc} */
    @Override
    public final void onReceivedText(String message) {
        System.out.println("Received: " + message);
        try {
            AbstractTextMessage m = AbstractTextMessage.read(message);
            m.setReceivedRawMesage(message);
            onReceivedText0(m);
        } catch (Throwable e) {
            e.printStackTrace();
            tryClose(5004, e.getMessage());
        }
    }

    protected void onReceivedText0(AbstractTextMessage m) {
        if (m instanceof AckMessage) {
            AckMessage am = (AckMessage) m;
            NetworkFutureImpl<?> f = ac.acks.remove(am.getMessageAck());
            if (f == null) {
                System.err.println("Orphaned packet with id " + am.getMessageAck() + " registered " + ac.acks.keySet()
                        + ", local " + "" + " p = ");
                // TODO close connection with error
            } else {
                try {
                    client().handleMessageReply(m, f);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            // System.out.println("RELEASING " + am.getMessageAck() + ", remaining " + acks.keySet());
        } else {
            try {
                client().handleMessage(m);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public final <T> NetworkFutureImpl<T> sendMessage(AbstractRelayedMessage m) {
        sendMessage((AbstractTextMessage) m);
        return null;
    }

    public final void sendMessage(AbstractTextMessage m) {
        sendRawTextMessage(m.toJSON());
    }

    public final <T> NetworkFutureImpl<T> sendMessage(ReplyMessage<T> m) {
        // we need to send the messages in the same order as they are numbered for now
        synchronized (ac.ai) {
            long id = ac.ai.incrementAndGet();
            NetworkFutureImpl<T> f = new NetworkFutureImpl<>(ses);
            ac.acks.put(id, f);
            m.setReplyTo(id);
            sendMessage((AbstractTextMessage) m);
            return f;
        }
    }

    public final void sendRawTextMessage(String m) {
        try {
            System.out.println("Sending " + m);
            sendText(m);
        } catch (Exception e) {
            onError(e);
            failed(e.getMessage());
            e.printStackTrace();
        }
    }

    void setConnection(AbstractConnection ac) {
        this.ac = ac;
        if (ac != null) {
            this.ses = ac.ses;
        }
    }

    public final void tryClose(int statusCode, String reason) {
        close(statusCode, reason);
    }

}
