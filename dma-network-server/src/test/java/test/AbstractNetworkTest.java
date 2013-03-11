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

import java.net.InetSocketAddress;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;

import test.util.ProxyTester;
import dk.dma.enav.communication.MaritimeNetworkConnection;
import dk.dma.enav.model.MaritimeId;
import dk.dma.navnet.client.MaritimeNetworkConnectionBuilder;
import dk.dma.navnet.server.ENavNetworkServer;

/**
 * 
 * @author Kasper Nielsen
 */
public class AbstractNetworkTest {
    final boolean useProxy;
    protected CopyOnWriteArraySet<MaritimeNetworkConnection> clients = new CopyOnWriteArraySet<>();

    ProxyTester pt;

    ENavNetworkServer si;

    ExecutorService es = Executors.newCachedThreadPool();

    public AbstractNetworkTest() {
        this(false);
    }

    public AbstractNetworkTest(boolean useProxy) {
        this.useProxy = useProxy;
    }

    protected MaritimeNetworkConnection newClient(MaritimeId id) throws Exception {
        MaritimeNetworkConnectionBuilder b = MaritimeNetworkConnectionBuilder.create(id);
        MaritimeNetworkConnection c = b.connect();
        clients.add(c);
        return c;
    }

    protected Future<MaritimeNetworkConnection> newClientAsync(MaritimeId id) throws Exception {
        final MaritimeNetworkConnectionBuilder b = MaritimeNetworkConnectionBuilder.create(id);
        return es.submit(new Callable<MaritimeNetworkConnection>() {

            @Override
            public MaritimeNetworkConnection call() throws Exception {
                MaritimeNetworkConnection c = b.connect();
                clients.add(c);
                return c;
            }
        });
    }

    protected Set<MaritimeNetworkConnection> newClients(int count) throws Exception {
        HashSet<Future<MaritimeNetworkConnection>> futures = new HashSet<>();
        for (int j = 0; j < count; j++) {
            futures.add(newClientAsync(MaritimeId.create("mmsi://1234" + j)));
        }
        HashSet<MaritimeNetworkConnection> result = new HashSet<>();
        for (Future<MaritimeNetworkConnection> f : futures) {
            result.add(f.get(3, TimeUnit.SECONDS));
        }
        return result;
    }

    @Before
    public void setup() throws Exception {
        if (useProxy) {
            si = new ENavNetworkServer(12222);
            pt = new ProxyTester(new InetSocketAddress(43234), new InetSocketAddress(12222));
            pt.start();
        } else {
            si = new ENavNetworkServer(43234);
        }
        si.start();

    }

    @After
    public void teardown() throws InterruptedException {
        for (final MaritimeNetworkConnection c : clients) {
            es.execute(new Runnable() {
                public void run() {
                    c.close();
                }
            });
        }
        for (final MaritimeNetworkConnection c : clients) {
            assertTrue(c.awaitTerminated(5, TimeUnit.SECONDS));
        }

        si.shutdown();
        es.shutdown();
        if (pt != null) {
            pt.shutdown();
        }
        assertTrue(es.awaitTermination(10, TimeUnit.SECONDS));

        assertTrue(si.awaitTerminated(10, TimeUnit.SECONDS));
    }
}
