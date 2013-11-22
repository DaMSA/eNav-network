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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import org.junit.Ignore;
import org.junit.Test;

import dk.dma.enav.maritimecloud.MaritimeCloudClient;
import dk.dma.enav.maritimecloud.broadcast.BroadcastListener;
import dk.dma.enav.maritimecloud.broadcast.BroadcastMessageHeader;
import dk.dma.enav.model.geometry.PositionTime;
import dk.dma.navnet.client.AbstractClientConnectionTest;
import dk.dma.navnet.client.broadcast.stubs.HelloWorld;
import dk.dma.navnet.messages.TransportMessage;
import dk.dma.navnet.messages.c2c.broadcast.BroadcastDeliver;
import dk.dma.navnet.messages.c2c.broadcast.BroadcastSend;


/**
 * 
 * @author Kasper Nielsen
 */
public class ReconnectTest extends AbstractClientConnectionTest {

    @Test
    public void messageId() throws Exception {
        MaritimeCloudClient c = createAndConnect();

        c.broadcast(new HelloWorld("hello"));
        BroadcastSend m = t.take(BroadcastSend.class);
        assertEquals(1L, m.getMessageId());
        assertEquals(0L, m.getLatestReceivedId());

        c.broadcast(new HelloWorld("hello"));
        m = t.take(BroadcastSend.class);
        assertEquals(2L, m.getMessageId());
        assertEquals(0L, m.getLatestReceivedId());
    }

    @Test
    public void messageIdMany() throws Exception {
        MaritimeCloudClient c = createAndConnect();
        for (int i = 0; i < 200; i++) {
            c.broadcast(new HelloWorld("hello"));
            BroadcastSend m = t.take(BroadcastSend.class);
            assertEquals(i + 1, m.getMessageId());
            assertEquals(0L, m.getLatestReceivedId());
        }
    }

    @Test
    public void messageAck() throws Exception {
        MaritimeCloudClient c = createAndConnect();
        final CountDownLatch cdl1 = new CountDownLatch(1);
        final CountDownLatch cdl3 = new CountDownLatch(3);
        c.broadcastListen(HelloWorld.class, new BroadcastListener<HelloWorld>() {
            public void onMessage(BroadcastMessageHeader header, HelloWorld broadcast) {
                cdl1.countDown();
                cdl3.countDown();
            }
        });

        BroadcastDeliver bm = new BroadcastDeliver(ID3, PositionTime.create(2, 1, 4),
                HelloWorld.class.getCanonicalName(), persistAndEscape(new HelloWorld("A")));
        bm.setLatestReceivedId(0);
        bm.setMessageId(1);
        t.send(bm);
        assertTrue(cdl1.await(1, TimeUnit.SECONDS));

        Thread.sleep(1000);
        c.broadcast(new HelloWorld("hello"));
        BroadcastSend m = t.take(BroadcastSend.class);
        assertEquals(1L, m.getMessageId());
        assertEquals(1L, m.getLatestReceivedId());

        bm.setLatestReceivedId(1);
        bm.setMessageId(2);
        t.send(bm);

        bm.setLatestReceivedId(1);
        bm.setMessageId(3);
        t.send(bm);

        assertTrue(cdl3.await(1, TimeUnit.SECONDS));
        c.broadcast(new HelloWorld("hello"));
        m = t.take(BroadcastSend.class);
        assertEquals(2L, m.getMessageId());
        assertEquals(3L, m.getLatestReceivedId());
    }

    /** Tests a mix of messages. */
    @Test
    @Ignore
    public void messageAckMany() throws Exception {
        MaritimeCloudClient c = createAndConnect();
        int count = 200;
        LinkedBlockingDeque<TransportMessage> bq = t.setQueue(new LinkedBlockingDeque<TransportMessage>());
        int lastestOut = 0;
        for (int i = 0; i < count; i++) {
            if (ThreadLocalRandom.current().nextBoolean()) {
                // server send message
                BroadcastDeliver bm = new BroadcastDeliver(ID3, PositionTime.create(2, 1, 4),
                        HelloWorld.class.getCanonicalName(), persistAndEscape(new HelloWorld("A")));
                bm.setLatestReceivedId(0);
                bm.setMessageId(++lastestOut);
                t.send(bm);
            } else {
                c.broadcast(new HelloWorld("hello"));
            }
        }
        for (int i = 0; i < count - lastestOut; i++) {
            assertNotNull(bq.poll(1, TimeUnit.SECONDS));
        }
        bq.clear();
        c.broadcast(new HelloWorld("hello"));
        BroadcastSend m = t.take(BroadcastSend.class);
        assertEquals(count - lastestOut + 1, m.getMessageId());
        assertEquals(lastestOut, m.getLatestReceivedId());
    }
}
