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
package dk.dma.enav.communication.broadcast;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import test.stubs.BroadcastTestMessage;
import dk.dma.enav.communication.AbstractNetworkTest;
import dk.dma.enav.communication.PersistentConnection;

/**
 * 
 * @author Kasper Nielsen
 */
public class BroadcastTest extends AbstractNetworkTest {

    @Test
    public void oneBroadcast() throws Exception {
        PersistentConnection c1 = newClient(ID1);
        PersistentConnection c2 = newClient(ID6);
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
        PersistentConnection c1 = newClient(ID1);
        final CountDownLatch cdl = new CountDownLatch(10);

        for (PersistentConnection mnc : newClients(10)) {
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
        PersistentConnection c1 = newClient(ID1);
        PersistentConnection c2 = newClient(ID6);
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
