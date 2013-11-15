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
package dk.dma.enav.communication;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.Ignore;
import org.junit.Test;

/**
 * Tests that if multiple clients connect with the same id connects. Only one is connected at the same time
 * 
 * @author Kasper Nielsen
 */
public class SameIDConnectTest extends AbstractNetworkTest {

    /**
     * Tests that an existing client will disconnect
     * 
     * @throws Exception
     */
    @Test
    public void twoConnect() throws Exception {
        MaritimeNetworkClient pc1 = newClient(ID1);
        pc1.connection().awaitConnected(1, TimeUnit.SECONDS);

        MaritimeNetworkClient pc2 = newClient(ID1);
        pc2.connection().awaitConnected(1, TimeUnit.SECONDS);

        // pc1 should be disconnected now
        assertEquals(1, si.info().getConnectionCount());
        pc1.connection().awaitDisconnected(1, TimeUnit.SECONDS);
        assertTrue(pc2.connection().isConnected());
        assertEquals(1, si.info().getConnectionCount());
    }

    @Test
    @Ignore
    public void manyConnect() throws Exception {
        ExecutorService e = Executors.newFixedThreadPool(10);
        final Set<Future<MaritimeNetworkClient>> s = Collections
                .newSetFromMap(new ConcurrentHashMap<Future<MaritimeNetworkClient>, Boolean>());
        for (int i = 0; i < 100; i++) {
            s.add(e.submit(new Callable<MaritimeNetworkClient>() {
                public MaritimeNetworkClient call() throws Exception {
                    return newClient(ID1);
                }
            }));
        }

        Set<MaritimeNetworkClient> con = new HashSet<>();
        for (Future<MaritimeNetworkClient> f : s) {
            try {
                con.add(f.get());
            } catch (ExecutionException ignore) {}
        }
        for (int i = 0; i < 100; i++) {
            if (si.info().getConnectionCount() == 1) {
                int connectCount = 0;
                for (MaritimeNetworkClient pc : con) {
                    if (pc.connection().isConnected()) {
                        connectCount++;
                    }
                }
                assertEquals(1, connectCount);
                return;
            }
            Thread.sleep(15);
        }
        fail("Number of connections = " + si.info().getConnectionCount());

        // TODO when we have asynchronous connect, there should be exactly 100 connections
    }
}
