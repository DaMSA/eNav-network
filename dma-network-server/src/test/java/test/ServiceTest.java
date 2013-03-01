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
package test;

import static org.junit.Assert.assertEquals;

import java.util.concurrent.TimeUnit;

import org.junit.Ignore;
import org.junit.Test;

import dk.dma.enav.model.MaritimeId;
import dk.dma.enav.net.MaritimeNetworkConnection;
import dk.dma.enav.net.ServiceCallback;
import dk.dma.enav.net.broadcast.BroadcastMessage;
import dk.dma.enav.service.ship.GetNameService;
import dk.dma.enav.service.ship.GetNameService.GetName;
import dk.dma.enav.service.ship.GetNameService.Reply;

/**
 * 
 * @author Kasper Nielsen
 */
@Ignore
public class ServiceTest extends AbstractNetworkTest {
    public static final MaritimeId ID1 = MaritimeId.create("mmsi://1");
    public static final MaritimeId ID6 = MaritimeId.create("mmsi://6");

    @Test
    @Ignore
    public void test() throws Exception {
        MaritimeNetworkConnection c = newClient(ID1);
        c.broadcast(new TestMsg());
        // Thread.sleep(1000);
    }

    @Test
    public void testService() throws Exception {
        MaritimeNetworkConnection c = newClient(ID1);
        c.registerService(new GetNameService(), new ServiceCallback<GetName, Reply>() {
            public void process(GetName l, Context<Reply> context) {
                context.complete(new Reply("testok"));
            }
        }).awaitRegistered(1, TimeUnit.SECONDS);
        assertEquals("testok", c.invokeService(ID1, new GetNameService.GetName()).get(1, TimeUnit.SECONDS).getName());
    }

    @Test
    public void testServiceOtherClient() throws Exception {
        MaritimeNetworkConnection c = newClient(ID1);
        MaritimeNetworkConnection c6 = newClient(ID6);
        c.registerService(new GetNameService(), new ServiceCallback<GetName, Reply>() {
            public void process(GetName l, Context<Reply> context) {
                context.complete(new Reply("testok"));
            }
        }).awaitRegistered(1, TimeUnit.SECONDS);
        assertEquals("testok", c6.invokeService(ID1, new GetNameService.GetName()).get(1, TimeUnit.SECONDS).getName());
    }

    static class TestMsg extends BroadcastMessage {

    }
}
