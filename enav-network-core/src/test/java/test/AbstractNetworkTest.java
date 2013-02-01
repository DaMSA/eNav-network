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

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;

import dk.dma.enav.model.MaritimeId;
import dk.dma.enav.net.MaritimeNetworkConnection;
import dk.dma.enav.network.MaritimeNetworkConnectionBuilder;
import dk.dma.enav.network.server.ENavNetworkServer;

/**
 * 
 * @author Kasper Nielsen
 */
public class AbstractNetworkTest {
    static boolean useProxy = false;
    CopyOnWriteArraySet<MaritimeNetworkConnection> clients = new CopyOnWriteArraySet<>();

    ProxyTester pt;

    ENavNetworkServer si;

    protected MaritimeNetworkConnection newClient(MaritimeId id) throws Exception {
        MaritimeNetworkConnectionBuilder b = MaritimeNetworkConnectionBuilder.create(id);
        MaritimeNetworkConnection c = b.connect();
        clients.add(c);
        return c;
    }

    @Before
    public void setup() throws IOException {
        if (useProxy) {
            si = new ENavNetworkServer(12222);
            pt = new ProxyTester(new InetSocketAddress(11111), new InetSocketAddress(12222));
            pt.start();
        } else {
            si = new ENavNetworkServer(11111);
        }
        si.start();

    }

    @After
    public void teardown() throws InterruptedException {
        for (MaritimeNetworkConnection c : clients) {
            c.close();
            assertTrue(c.awaitFullyClosed(5, TimeUnit.SECONDS));
        }

        si.shutdown();
        if (pt != null) {
            pt.shutdown();
        }
        assertTrue(si.awaitTerminated(10, TimeUnit.SECONDS));
    }
}
