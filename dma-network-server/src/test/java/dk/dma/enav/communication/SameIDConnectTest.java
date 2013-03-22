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

import java.util.concurrent.TimeUnit;

import org.junit.Test;

import dk.dma.enav.communication.PersistentConnection.State;
import dk.dma.enav.model.MaritimeId;

/**
 * Tests that if multiple clients connect with the same id connects. Only one is connected at the same time
 * 
 * @author Kasper Nielsen
 */
public class SameIDConnectTest extends AbstractNetworkTest {
    public static final MaritimeId ID1 = MaritimeId.create("mmsi://1");

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
}
