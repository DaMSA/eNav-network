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

import java.util.concurrent.TimeUnit;

import org.junit.Test;

import test.stubs.HelloService;
import test.stubs.HelloService.GetName;
import test.stubs.HelloService.Reply;
import dk.dma.enav.communication.AbstractNetworkTest;
import dk.dma.enav.communication.MaritimeNetworkConnection;
import dk.dma.enav.communication.NetworkFuture;
import dk.dma.enav.model.MaritimeId;

/**
 * 
 * @author Kasper Nielsen
 */
public class ServiceTest extends AbstractNetworkTest {
    public static final MaritimeId ID1 = MaritimeId.create("mmsi://1");
    public static final MaritimeId ID6 = MaritimeId.create("mmsi://6");

    @Test
    public void oneClient() throws Exception {
        MaritimeNetworkConnection c1 = newClient(ID1);
        c1.serviceRegister(HelloService.GET_NAME, new InvocationCallback<HelloService.GetName, HelloService.Reply>() {
            public void process(GetName message, InvocationCallback.Context<Reply> context) {
                context.complete(new Reply("foo123"));
            }
        }).awaitRegistered(4, TimeUnit.SECONDS);

        MaritimeNetworkConnection c2 = newClient(ID6);
        ServiceEndpoint<GetName, Reply> end = c2.serviceFindOne(HelloService.GET_NAME).get(6, TimeUnit.SECONDS);
        assertEquals(ID1, end.getId());
        NetworkFuture<Reply> f = end.invoke(new HelloService.GetName());
        assertEquals("foo123", f.get(4, TimeUnit.SECONDS).getName());
    }

    @Test
    public void manyClients() throws Exception {
        MaritimeNetworkConnection c1 = newClient(ID1);
        c1.serviceRegister(HelloService.GET_NAME, new InvocationCallback<HelloService.GetName, HelloService.Reply>() {
            public void process(GetName message, InvocationCallback.Context<Reply> context) {
                context.complete(new Reply("foo123"));
            }
        }).awaitRegistered(4, TimeUnit.SECONDS);

        for (MaritimeNetworkConnection c : newClients(20)) {
            ServiceEndpoint<GetName, Reply> end = c.serviceFindOne(HelloService.GET_NAME).get(6, TimeUnit.SECONDS);
            assertEquals(ID1, end.getId());
            NetworkFuture<Reply> f = end.invoke(new HelloService.GetName());
            assertEquals("foo123", f.get(4, TimeUnit.SECONDS).getName());

        }
    }
}
