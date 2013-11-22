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
package dk.dma.navnet.server.connection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

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
public class ConnectTest extends AbstractServerConnectionTest {

    @Test
    public void connectTest() throws Exception {
        TesstEndpoint t = newClient();
        WelcomeMessage wm = t.take(WelcomeMessage.class);
        assertNotNull(wm.getServerId());
        t.send(new HelloMessage(ID2, "foo", "", 0, 1, 1));

        ConnectedMessage cm = t.take(ConnectedMessage.class);
        assertEquals(0, cm.getLastReceivedMessageId());
    }


    @Test
    public void messageId() throws Exception {
        TesstEndpoint t = newClient();
        t.take(WelcomeMessage.class);
        t.send(new HelloMessage(ID2, "foo", "", 0, 1, 1));
        ConnectedMessage cm = t.take(ConnectedMessage.class);
        assertEquals(0, cm.getLastReceivedMessageId());

        ConnectionMessage bs = BroadcastSend.create(ID1, PositionTime.create(1, 1, 1), new HelloWorld("foo1"),
                new BroadcastOptions()).setReplyTo(1234);
        bs.setMessageId(1);
        t.send(bs);

        BroadcastSendAck bd = t.take(BroadcastSendAck.class);
        assertEquals(1, bd.getMessageId());
        assertEquals(1, bd.getLatestReceivedId());
    }

    @Test
    public void messageId2() throws Exception {
        TesstEndpoint t = newClient();
        t.take(WelcomeMessage.class);
        t.send(new HelloMessage(ID2, "foo", "", 0, 1, 1));
        ConnectedMessage cm = t.take(ConnectedMessage.class);
        assertEquals(0, cm.getLastReceivedMessageId());

        ConnectionMessage bs = BroadcastSend.create(ID1, PositionTime.create(1, 1, 1), new HelloWorld("foo1"),
                new BroadcastOptions()).setReplyTo(1234);
        bs.setMessageId(1);
        t.send(bs);

        BroadcastSendAck bd = t.take(BroadcastSendAck.class);
        assertEquals(1, bd.getMessageId());
        assertEquals(1, bd.getLatestReceivedId());

        bs = BroadcastSend.create(ID1, PositionTime.create(1, 1, 1), new HelloWorld("foo1"), new BroadcastOptions())
                .setReplyTo(1234);
        bs.setMessageId(2);
        bs.setLatestReceivedId(2);
        t.send(bs);

        bd = t.take(BroadcastSendAck.class);
        assertEquals(2, bd.getMessageId());
        assertEquals(2, bd.getLatestReceivedId());
    }
}
