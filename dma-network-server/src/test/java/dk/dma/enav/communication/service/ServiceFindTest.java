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
package dk.dma.enav.communication.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import test.stubs.HelloService;
import test.stubs.HelloService.GetName;
import test.stubs.HelloService.Reply;
import dk.dma.enav.communication.PersistentNetworkConnection;
import dk.dma.enav.model.geometry.CoordinateSystem;
import dk.dma.enav.model.geometry.Position;

/**
 * 
 * @author Kasper Nielsen
 */
public class ServiceFindTest extends AbstractServiceTest {

    /**
     * Tests that one ship cannot find it self.
     * 
     * @throws Exception
     */
    @Test
    public void findNearest() throws Exception {
        PersistentNetworkConnection s = registerService(newClient(), "foo123");
        PersistentNetworkConnection c = newClient();
        ServiceEndpoint<GetName, Reply> end = c.serviceFind(HelloService.GET_NAME).nearest().get(6, TimeUnit.SECONDS);
        assertEquals(s.getLocalId(), end.getId());
    }

    @Test
    public void findNearestOutOf2() throws Exception {
        PersistentNetworkConnection s1 = registerService(newClient(1, 1), "A");
        PersistentNetworkConnection s2 = registerService(newClient(5, 5), "B");
        ServiceEndpoint<GetName, Reply> e;

        e = newClient(2, 2).serviceFind(HelloService.GET_NAME).nearest().get(6, TimeUnit.SECONDS);
        assertEquals(s1.getLocalId(), e.getId());

        e = newClient(3, 4).serviceFind(HelloService.GET_NAME).nearest().get(6, TimeUnit.SECONDS);
        assertEquals(s2.getLocalId(), e.getId());

        e = newClient(4, 4).serviceFind(HelloService.GET_NAME).nearest().get(6, TimeUnit.SECONDS);
        assertEquals(s2.getLocalId(), e.getId());
    }

    @Test
    public void findNearestOfMany() throws Exception {
        Map<Integer, PersistentNetworkConnection> m = new HashMap<>();
        for (int i = 0; i < 10; i++) {
            m.put(i, registerService(newClient(i, i), "A" + i));
        }

        List<ServiceEndpoint<GetName, Reply>> e;

        e = newClient(4.4, 4.4).serviceFind(HelloService.GET_NAME).nearest(1).get(6, TimeUnit.SECONDS);
        assertEquals(1, e.size());
        assertEquals(e.get(0).getId(), m.get(4).getLocalId());

        e = newClient(5.4, 5.4).serviceFind(HelloService.GET_NAME).nearest(3).get(6, TimeUnit.SECONDS);
        assertEquals(3, e.size());
        assertEquals(e.get(0).getId(), m.get(5).getLocalId());
        assertEquals(e.get(1).getId(), m.get(6).getLocalId());
        assertEquals(e.get(2).getId(), m.get(4).getLocalId());
    }

    /**
     * Tests that one ship cannot find it self as a service.
     * 
     * @throws Exception
     */
    @Test
    public void cannotFindSelf() throws Exception {
        PersistentNetworkConnection s = registerService(newClient(), "foo123");
        assertNull(s.serviceFind(HelloService.GET_NAME).nearest().get(6, TimeUnit.SECONDS));
    }

    @Test
    public void foo() {
        System.out.println("XXXXXXXXXXXXXXX "
                + Position.create(4.4, 4.4).distanceTo(Position.create(4, 4), CoordinateSystem.GEODETIC));
        System.out.println("XXXXXXXXXXXXXXX "
                + Position.create(4.4, 4.4).distanceTo(Position.create(5, 5), CoordinateSystem.GEODETIC));

    }

    @Test
    public void findWithMaxDistance() throws Exception {
        Map<Integer, PersistentNetworkConnection> m = new HashMap<>();
        for (int i = 0; i < 10; i++) {
            m.put(i, registerService(newClient(i, i), "A" + i));
        }

        List<ServiceEndpoint<GetName, Reply>> e;

        e = newClient(4.4, 4.4).serviceFind(HelloService.GET_NAME).withinDistanceOf(62678).nearest(2)
                .get(6, TimeUnit.SECONDS);
        assertEquals(0, e.size());

        e = newClient(4.4, 4.4).serviceFind(HelloService.GET_NAME).withinDistanceOf(62679).nearest(2)
                .get(6, TimeUnit.SECONDS);
        assertEquals(1, e.size());
        assertEquals(e.get(0).getId(), m.get(4).getLocalId());
    }
}
