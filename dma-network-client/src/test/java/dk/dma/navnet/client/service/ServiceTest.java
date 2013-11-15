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
package dk.dma.navnet.client.service;

import static org.junit.Assert.assertEquals;

import java.util.concurrent.TimeUnit;

import org.junit.Test;

import test.stubs.HelloService;
import test.stubs.HelloService.GetName;
import test.stubs.HelloService.Reply;
import dk.dma.enav.communication.ConnectionFuture;
import dk.dma.enav.communication.MaritimeNetworkClient;
import dk.dma.enav.communication.service.ServiceEndpoint;
import dk.dma.enav.communication.service.ServiceRegistration;
import dk.dma.enav.model.MaritimeId;
import dk.dma.navnet.client.AbstractClientConnectionTest;
import dk.dma.navnet.messages.c2c.service.InvokeService;
import dk.dma.navnet.messages.s2c.service.FindService;
import dk.dma.navnet.messages.s2c.service.RegisterService;

/**
 * 
 * @author Kasper Nielsen
 */
public class ServiceTest extends AbstractClientConnectionTest {

    @Test
    public void register() throws Exception {
        MaritimeNetworkClient c = createConnect();
        ServiceRegistration sr = c.serviceRegister(HelloService.GET_NAME, HelloService.create("ok"));

        RegisterService rs = t.take(RegisterService.class);
        assertEquals(HelloService.GET_NAME.getName(), rs.getServiceName());
        t.send(rs.createReply());


        sr.awaitRegistered(10, TimeUnit.SECONDS);

        // c.serviceRegister(null, null)
        //
        // MaritimeNetworkClient s = registerService(newClient(), "foo123");
        // MaritimeNetworkClient c = newClient();
        // ServiceEndpoint<GetName, Reply> end = c.serviceLocate(HelloService.GET_NAME).nearest().get(6,
        // TimeUnit.SECONDS);
        // assertEquals(s.getClientId(), end.getId());
    }

    @Test
    public void locate() throws Exception {
        MaritimeNetworkClient c = createConnect();
        ConnectionFuture<ServiceEndpoint<GetName, Reply>> locator = c.serviceLocate(HelloService.GET_NAME).nearest();

        FindService rs = t.take(FindService.class);
        assertEquals(HelloService.GET_NAME.getName(), rs.getServiceName());
        t.send(rs.createReply(new String[] { "mmsi://4321" }));


        ServiceEndpoint<GetName, Reply> se = locator.get(1, TimeUnit.SECONDS);
        assertEquals(MaritimeId.create("mmsi://4321"), se.getId());


        ConnectionFuture<Reply> invoke = se.invoke(new GetName());
        InvokeService is = t.take(InvokeService.class);
        t.send(is.createReply(new Reply("okfoo")));


        assertEquals("okfoo", invoke.get().getName());
    }
}
