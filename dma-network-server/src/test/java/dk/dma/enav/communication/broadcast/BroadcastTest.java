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
package dk.dma.enav.communication.broadcast;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import test.stubs.BroadcastTestMessage;
import dk.dma.enav.communication.AbstractNetworkTest;
import dk.dma.enav.communication.MaritimeNetworkConnection;

/**
 * 
 * @author Kasper Nielsen
 */
public class BroadcastTest extends AbstractNetworkTest {

    @Test
    public void oneBroadcast() throws Exception {
        MaritimeNetworkConnection c1 = newClient(ID1);
        MaritimeNetworkConnection c2 = newClient(ID6);
        final CountDownLatch cdl = new CountDownLatch(1);
        c2.broadcastListen(BroadcastTestMessage.class, new BroadcastListener<BroadcastTestMessage>() {
            public void onMessage(BroadcastMessageHeader props, BroadcastTestMessage t) {
                assertEquals("fooo", t.getName());
                cdl.countDown();
            }
        });

        c1.broadcast(new BroadcastTestMessage("fooo"));
        assertTrue(cdl.await(4, TimeUnit.SECONDS));
    }

    @Test
    public void multipleReceivers() throws Exception {
        MaritimeNetworkConnection c1 = newClient(ID1);
        final CountDownLatch cdl = new CountDownLatch(10);

        for (MaritimeNetworkConnection mnc : newClients(10)) {
            mnc.broadcastListen(BroadcastTestMessage.class, new BroadcastListener<BroadcastTestMessage>() {
                public void onMessage(BroadcastMessageHeader props, BroadcastTestMessage t) {
                    assertEquals("fooo", t.getName());
                    cdl.countDown();
                }
            });
        }
        c1.broadcast(new BroadcastTestMessage("fooo"));
        assertTrue(cdl.await(4, TimeUnit.SECONDS));
    }

    @Test
    public void receiveNotSelf() throws Exception {
        MaritimeNetworkConnection c1 = newClient(ID1);
        MaritimeNetworkConnection c2 = newClient(ID6);
        final CountDownLatch cdl1 = new CountDownLatch(1);
        final CountDownLatch cdl2 = new CountDownLatch(1);
        c1.broadcastListen(BroadcastTestMessage.class, new BroadcastListener<BroadcastTestMessage>() {
            public void onMessage(BroadcastMessageHeader properties, BroadcastTestMessage t) {
                assertEquals("fooo", t.getName());
                cdl1.countDown();
            }
        });
        c2.broadcastListen(BroadcastTestMessage.class, new BroadcastListener<BroadcastTestMessage>() {
            public void onMessage(BroadcastMessageHeader properties, BroadcastTestMessage t) {
                assertEquals("fooo", t.getName());
                cdl2.countDown();
            }
        });

        c1.broadcast(new BroadcastTestMessage("fooo"));
        assertTrue(cdl2.await(4, TimeUnit.SECONDS));
        assertFalse(cdl1.await(20, TimeUnit.MILLISECONDS));
    }

}
