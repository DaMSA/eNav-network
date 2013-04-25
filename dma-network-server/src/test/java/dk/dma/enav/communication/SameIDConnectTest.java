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
package dk.dma.enav.communication;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
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

import org.junit.Test;

import dk.dma.enav.communication.PersistentConnection.State;

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
        PersistentConnection pc1 = newClient(ID1);
        pc1.awaitState(State.CONNECTED, 1, TimeUnit.SECONDS);
        PersistentConnection pc2 = newClient(ID1);
        pc2.awaitState(State.CONNECTED, 1, TimeUnit.SECONDS);
        assertEquals(1, si.getNumberOfConnections());
        assertTrue(pc1.awaitState(State.CLOSED, 1, TimeUnit.SECONDS));
        assertSame(State.CONNECTED, pc2.getState());
        assertEquals(1, si.getNumberOfConnections());
    }

    @Test
    public void manyConnect() throws Exception {
        ExecutorService e = Executors.newFixedThreadPool(10);
        final Set<Future<PersistentConnection>> s = Collections
                .newSetFromMap(new ConcurrentHashMap<Future<PersistentConnection>, Boolean>());
        for (int i = 0; i < 100; i++) {
            s.add(e.submit(new Callable<PersistentConnection>() {
                public PersistentConnection call() throws Exception {
                    return newClient(ID1);
                }
            }));
        }

        Set<PersistentConnection> con = new HashSet<>();
        for (Future<PersistentConnection> f : s) {
            try {
                con.add(f.get());
            } catch (ExecutionException ignore) {}
        }
        for (int i = 0; i < 100; i++) {
            if (si.getNumberOfConnections() == 1) {
                int connectCount = 0;
                for (PersistentConnection pc : con) {
                    if (pc.getState() == State.CONNECTED) {
                        connectCount++;
                    }
                }
                assertEquals(1, connectCount);
                return;
            }
            Thread.sleep(15);
        }
        fail("Number of connections = " + si.getNumberOfConnections());

        // TODO when we have asynchronous connect, there should be exactly 100 connections

    }
}
