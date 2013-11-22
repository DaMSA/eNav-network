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
import dk.dma.enav.maritimecloud.MaritimeCloudClient;
import dk.dma.enav.maritimecloud.service.ServiceEndpoint;
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
        MaritimeCloudClient s = registerService(newClient(), "foo123");
        MaritimeCloudClient c = newClient();
        ServiceEndpoint<GetName, Reply> end = c.serviceLocate(HelloService.GET_NAME).nearest().get(6, TimeUnit.SECONDS);
        assertEquals(s.getClientId(), end.getId());
    }

    @Test
    public void findNearestOutOf2() throws Exception {
        MaritimeCloudClient s1 = registerService(newClient(1, 1), "A");
        MaritimeCloudClient s2 = registerService(newClient(5, 5), "B");
        ServiceEndpoint<GetName, Reply> e;

        e = newClient(2, 2).serviceLocate(HelloService.GET_NAME).nearest().get(6, TimeUnit.SECONDS);
        assertEquals(s1.getClientId(), e.getId());

        e = newClient(3, 4).serviceLocate(HelloService.GET_NAME).nearest().get(6, TimeUnit.SECONDS);
        assertEquals(s2.getClientId(), e.getId());

        e = newClient(4, 4).serviceLocate(HelloService.GET_NAME).nearest().get(6, TimeUnit.SECONDS);
        assertEquals(s2.getClientId(), e.getId());
    }

    @Test
    public void findNearestOfMany() throws Exception {
        Map<Integer, MaritimeCloudClient> m = new HashMap<>();
        for (int i = 0; i < 10; i++) {
            m.put(i, registerService(newClient(i, i), "A" + i));
        }

        List<ServiceEndpoint<GetName, Reply>> e;

        e = newClient(4.4, 4.4).serviceLocate(HelloService.GET_NAME).nearest(1).get(6, TimeUnit.SECONDS);
        assertEquals(1, e.size());
        assertEquals(e.get(0).getId(), m.get(4).getClientId());

        e = newClient(5.4, 5.4).serviceLocate(HelloService.GET_NAME).nearest(3).get(6, TimeUnit.SECONDS);
        assertEquals(3, e.size());
        assertEquals(e.get(0).getId(), m.get(5).getClientId());
        assertEquals(e.get(1).getId(), m.get(6).getClientId());
        assertEquals(e.get(2).getId(), m.get(4).getClientId());
    }

    /**
     * Tests that one ship cannot find it self as a service.
     * 
     * @throws Exception
     */
    @Test
    public void cannotFindSelf() throws Exception {
        MaritimeCloudClient s = registerService(newClient(), "foo123");
        assertNull(s.serviceLocate(HelloService.GET_NAME).nearest().get(6, TimeUnit.SECONDS));
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
        Map<Integer, MaritimeCloudClient> m = new HashMap<>();
        for (int i = 0; i < 10; i++) {
            m.put(i, registerService(newClient(i, i), "A" + i));
        }

        List<ServiceEndpoint<GetName, Reply>> e;

        e = newClient(4.4, 4.4).serviceLocate(HelloService.GET_NAME).withinDistanceOf(62678).nearest(2)
                .get(6, TimeUnit.SECONDS);
        assertEquals(0, e.size());

        e = newClient(4.4, 4.4).serviceLocate(HelloService.GET_NAME).withinDistanceOf(62679).nearest(2)
                .get(6, TimeUnit.SECONDS);
        assertEquals(1, e.size());
        assertEquals(e.get(0).getId(), m.get(4).getClientId());
    }
}
