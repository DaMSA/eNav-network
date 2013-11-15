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
package dk.dma.navnet.client.broadcast;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Ignore;
import org.junit.Test;

import dk.dma.enav.communication.MaritimeNetworkClient;
import dk.dma.enav.communication.broadcast.BroadcastListener;
import dk.dma.enav.communication.broadcast.BroadcastMessage;
import dk.dma.enav.communication.broadcast.BroadcastMessageHeader;
import dk.dma.enav.model.geometry.PositionTime;
import dk.dma.navnet.client.AbstractClientConnectionTest;
import dk.dma.navnet.client.broadcast.stubs.HelloWorld;
import dk.dma.navnet.client.broadcast.stubs.HelloWorld2;
import dk.dma.navnet.messages.c2c.broadcast.BroadcastDeliver;
import dk.dma.navnet.messages.c2c.broadcast.BroadcastSend;

/**
 * 
 * @author Kasper Nielsen
 */
public class BroadcastTest extends AbstractClientConnectionTest {

    @Test
    public void broadcast() throws Exception {
        MaritimeNetworkClient c = createConnect();

        c.broadcast(new HelloWorld("hello"));

        BroadcastSend mb = t.take(BroadcastSend.class);
        HelloWorld hw = (HelloWorld) mb.tryRead();
        assertEquals("hello", hw.getMessage());
    }

    @Test
    public void broadcastListen() throws Exception {
        MaritimeNetworkClient c = createConnect();

        final CountDownLatch cdl = new CountDownLatch(1);
        c.broadcastListen(HelloWorld.class, new BroadcastListener<HelloWorld>() {
            public void onMessage(BroadcastMessageHeader header, HelloWorld broadcast) {
                assertEquals(ID2, header.getId());
                assertEquals(PositionTime.create(1, 1, 1), header.getPosition());
                assertEquals("foo$\\\n", broadcast.getMessage());
                cdl.countDown();
            }
        });

        // the first message should not be send to the handler
        HelloWorld2 hw2 = new HelloWorld2("NOTNT");
        BroadcastDeliver m = new BroadcastDeliver(ID3, PositionTime.create(1, 1, 1),
                HelloWorld2.class.getCanonicalName(), persistAndEscape(hw2));

        HelloWorld hw = new HelloWorld("foo$\\\n");
        m = new BroadcastDeliver(ID2, PositionTime.create(1, 1, 1), HelloWorld.class.getCanonicalName(),
                persistAndEscape(hw));
        t.send(m);

        assertTrue(cdl.await(2, TimeUnit.SECONDS));
    }

    @Test
    @Ignore
    // Subtype does not work, probably never will
    // Its OOD and
    public void broadcastListenSubType() throws Exception {
        MaritimeNetworkClient c = createConnect();

        final CountDownLatch cdl = new CountDownLatch(2);
        c.broadcastListen(BroadcastMessage.class, new BroadcastListener<BroadcastMessage>() {
            public void onMessage(BroadcastMessageHeader header, BroadcastMessage broadcast) {
                if (cdl.getCount() == 2) {
                    HelloWorld2 hw = (HelloWorld2) broadcast;
                    assertEquals(ID3, header.getId());
                    assertEquals(PositionTime.create(2, 1, 4), header.getPosition());
                    assertEquals("NOTNT", hw.getMessage());
                } else if (cdl.getCount() == 1) {
                    HelloWorld hw = (HelloWorld) broadcast;
                    assertEquals(ID2, header.getId());
                    assertEquals(PositionTime.create(1, 1, 1), header.getPosition());
                    assertEquals("foo$\\\n", hw.getMessage());
                } else {
                    throw new AssertionError();
                }
                cdl.countDown();
            }
        });

        // the first message should not be send to the handler
        HelloWorld2 hw2 = new HelloWorld2("NOTNT");
        BroadcastDeliver m = new BroadcastDeliver(ID3, PositionTime.create(2, 1, 4),
                HelloWorld2.class.getCanonicalName(), persistAndEscape(hw2));

        HelloWorld hw = new HelloWorld("foo$\\\n");
        m = new BroadcastDeliver(ID2, PositionTime.create(1, 1, 1), HelloWorld.class.getCanonicalName(),
                persistAndEscape(hw));
        t.send(m);

        assertTrue(cdl.await(2, TimeUnit.SECONDS));
    }
}
