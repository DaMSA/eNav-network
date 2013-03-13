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

import org.junit.Test;

import dk.dma.enav.model.MaritimeId;

/**
 * 
 * @author Kasper Nielsen
 */
public class ConnectionTest extends AbstractNetworkTest {
    public static final MaritimeId ID1 = MaritimeId.create("mmsi://1");
    public static final MaritimeId ID6 = MaritimeId.create("mmsi://6");

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

}
