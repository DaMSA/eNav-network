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
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.concurrent.TimeUnit;

import org.junit.Test;

import dk.dma.enav.communication.PersistentConnection.State;

/**
 * 
 * @author Kasper Nielsen
 */
public class ConnectionTest extends AbstractNetworkTest {

    @Test
    public void manyClients() throws Exception {
        newClients(20);
        assertEquals(20, si.getNumberOfConnections());
    }

    @Test
    public void singleClient() throws Exception {
        newClient(ID1);
        assertEquals(1, si.getNumberOfConnections());
        // Thread.sleep(1000);
        // assertEquals(1, si.getNumberOfConnections());
    }

    @Test
    public void singleClientClose() throws Exception {
        @SuppressWarnings("resource")
        PersistentConnection pc = newClient(ID1);
        assertEquals(1, si.getNumberOfConnections());
        pc.awaitState(State.CONNECTED, 1, TimeUnit.SECONDS);
        assertEquals(1, si.getNumberOfConnections());
        pc.close();
        assertTrue(pc.getState() == State.CLOSED || pc.getState() == State.TERMINATED);
        pc.awaitState(State.TERMINATED, 1, TimeUnit.SECONDS);
        for (int i = 0; i < 100; i++) {
            if (si.getNumberOfConnections() == 0) {
                return;
            }
            Thread.sleep(15);
        }
        fail();
    }
}
