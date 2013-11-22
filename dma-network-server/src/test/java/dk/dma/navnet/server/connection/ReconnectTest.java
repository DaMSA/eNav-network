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

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import dk.dma.enav.maritimecloud.broadcast.BroadcastOptions;
import dk.dma.enav.model.geometry.PositionTime;
import dk.dma.navnet.client.broadcast.stubs.HelloWorld;
import dk.dma.navnet.messages.ConnectionMessage;
import dk.dma.navnet.messages.auxiliary.ConnectedMessage;
import dk.dma.navnet.messages.auxiliary.HelloMessage;
import dk.dma.navnet.messages.auxiliary.WelcomeMessage;
import dk.dma.navnet.messages.c2c.broadcast.BroadcastSend;
import dk.dma.navnet.messages.c2c.broadcast.BroadcastSendAck;
import dk.dma.navnet.server.AbstractServerConnectionTest;
import dk.dma.navnet.server.TesstEndpoint;

/**
 * 
 * @author Kasper Nielsen
 */
public class ReconnectTest extends AbstractServerConnectionTest {

    @Test
    public void reconnect1() throws Exception {
        TesstEndpoint t = newClient();
        t.take(WelcomeMessage.class);

        t.send(new HelloMessage(ID2, "foo", "", 0, 1, 1));
        ConnectedMessage cm = t.take(ConnectedMessage.class);
        String reconnectId = cm.getConnectionId();
        assertEquals(0, cm.getLastReceivedMessageId());

        ConnectionMessage bs = BroadcastSend.create(ID1, PositionTime.create(1, 1, 1), new HelloWorld("foo1"),
                new BroadcastOptions()).setReplyTo(1234);
        bs.setMessageId(1);
        t.send(bs);

        BroadcastSendAck bd = t.take(BroadcastSendAck.class);
        assertEquals(1, bd.getMessageId());
        assertEquals(1, bd.getLatestReceivedId());

        t.close();

        t = newClient();
        t.take(WelcomeMessage.class);
        t.send(new HelloMessage(ID2, "foo", reconnectId, 1, 1, 1));
        cm = t.take(ConnectedMessage.class);
        assertEquals(reconnectId, cm.getConnectionId());
        assertEquals(1, cm.getLastReceivedMessageId());

        bs = BroadcastSend.create(ID1, PositionTime.create(1, 1, 1), new HelloWorld("foo1"), new BroadcastOptions())
                .setReplyTo(1234);
        bs.setMessageId(2);
        bs.setLatestReceivedId(1);
        t.send(bs);

        bd = t.take(BroadcastSendAck.class);
        assertEquals(2, bd.getMessageId());
        assertEquals(2, bd.getLatestReceivedId());
    }

    @Test
    public void connectTest() throws Exception {
        TesstEndpoint t = newClient();
        t.take(WelcomeMessage.class);

        t.send(new HelloMessage(ID2, "foo", "", 0, 1, 1));
        ConnectedMessage cm = t.take(ConnectedMessage.class);
        String reconnectId = cm.getConnectionId();
        assertEquals(0, cm.getLastReceivedMessageId());

        ConnectionMessage bs = BroadcastSend.create(ID1, PositionTime.create(1, 1, 1), new HelloWorld("foo1"),
                new BroadcastOptions()).setReplyTo(1234);
        bs.setMessageId(1);
        t.send(bs);

        BroadcastSendAck bd = t.take(BroadcastSendAck.class);
        assertEquals(1, bd.getMessageId());
        assertEquals(1, bd.getLatestReceivedId());

        t.close();

        t = newClient();
        t.take(WelcomeMessage.class);
        t.send(new HelloMessage(ID2, "foo", reconnectId, 0, 1, 1));
        cm = t.take(ConnectedMessage.class);
        assertEquals(reconnectId, cm.getConnectionId());
        assertEquals(1, cm.getLastReceivedMessageId());

        bd = t.take(BroadcastSendAck.class);
        assertEquals(1, bd.getMessageId());
        assertEquals(1, bd.getLatestReceivedId());

        bs = BroadcastSend.create(ID1, PositionTime.create(1, 1, 1), new HelloWorld("foo2"), new BroadcastOptions())
                .setReplyTo(1234);
        bs.setMessageId(2);
        bs.setLatestReceivedId(1);
        t.send(bs);

        bd = t.take(BroadcastSendAck.class);
        assertEquals(2, bd.getMessageId());
        assertEquals(2, bd.getLatestReceivedId());
    }
}
