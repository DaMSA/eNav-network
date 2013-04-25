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

import static org.junit.Assert.assertTrue;

import java.net.InetSocketAddress;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;

import test.util.ProxyTester;
import dk.dma.enav.communication.PersistentConnection.State;
import dk.dma.enav.model.MaritimeId;
import dk.dma.enav.model.geometry.PositionTime;
import dk.dma.enav.util.function.Supplier;
import dk.dma.navnet.client.MaritimeNetworkConnectionBuilder;
import dk.dma.navnet.server.ENavNetworkServer;

/**
 * 
 * @author Kasper Nielsen
 */
public class AbstractNetworkTest {

    public static final MaritimeId ID1 = MaritimeId.create("mmsi://1");
    public static final MaritimeId ID2 = MaritimeId.create("mmsi://2");
    public static final MaritimeId ID3 = MaritimeId.create("mmsi://3");
    public static final MaritimeId ID4 = MaritimeId.create("mmsi://4");
    public static final MaritimeId ID5 = MaritimeId.create("mmsi://5");
    public static final MaritimeId ID6 = MaritimeId.create("mmsi://6");

    protected final ConcurrentHashMap<MaritimeId, PersistentConnection> clients = new ConcurrentHashMap<>();
    ExecutorService es = Executors.newCachedThreadPool();

    protected final ConcurrentHashMap<MaritimeId, LocationSup> locs = new ConcurrentHashMap<>();

    ProxyTester pt;

    ENavNetworkServer si;

    final boolean useProxy;

    public AbstractNetworkTest() {
        this(false);
    }

    public AbstractNetworkTest(boolean useProxy) {
        this.useProxy = useProxy;
    }

    protected PersistentConnection newClient() throws Exception {
        for (;;) {
            MaritimeId id = MaritimeId.create("mmsi://" + ThreadLocalRandom.current().nextInt(1000));
            if (!clients.containsKey(id)) {
                return newClient(id);
            }
        }
    }

    protected PersistentConnection newClient(double lat, double lon) throws Exception {
        for (;;) {
            MaritimeId id = MaritimeId.create("mmsi://" + ThreadLocalRandom.current().nextInt(1000));
            if (!clients.containsKey(id)) {
                return newClient(id, lat, lon);
            }
        }
    }

    protected PersistentConnection newClient(MaritimeNetworkConnectionBuilder b) throws Exception {
        locs.put(b.getId(), new LocationSup());
        PersistentConnection c = b.build();
        clients.put(b.getId(), c);
        return c;
    }

    protected PersistentConnection newClient(MaritimeId id) throws Exception {
        MaritimeNetworkConnectionBuilder b = newBuilder(id);
        locs.put(id, new LocationSup());
        PersistentConnection c = b.build();
        clients.put(id, c);
        return c;
    }

    protected PersistentConnection newClient(MaritimeId id, double lat, double lon) throws Exception {
        MaritimeNetworkConnectionBuilder b = newBuilder(id);
        LocationSup ls = new LocationSup();
        b.setPositionSupplier(ls);
        locs.put(id, ls);
        setPosition(id, lat, lon);
        PersistentConnection c = b.build();
        clients.put(id, c);
        return c;
    }

    protected Future<PersistentConnection> newClientAsync(final MaritimeId id) throws Exception {
        final MaritimeNetworkConnectionBuilder b = newBuilder(id);
        locs.put(id, new LocationSup());
        return es.submit(new Callable<PersistentConnection>() {

            @Override
            public PersistentConnection call() throws Exception {
                PersistentConnection c = b.build();
                clients.put(id, c);
                return c;
            }
        });
    }

    protected MaritimeNetworkConnectionBuilder newBuilder(MaritimeId id) {
        MaritimeNetworkConnectionBuilder b = MaritimeNetworkConnectionBuilder.create(id);
        b.setHost("localhost:" + clientPort);
        return b;
    }

    protected Set<PersistentConnection> newClients(int count) throws Exception {
        HashSet<Future<PersistentConnection>> futures = new HashSet<>();
        for (int j = 0; j < count; j++) {
            futures.add(newClientAsync(MaritimeId.create("mmsi://1234" + j)));
        }
        HashSet<PersistentConnection> result = new HashSet<>();
        for (Future<PersistentConnection> f : futures) {
            result.add(f.get(3, TimeUnit.SECONDS));
        }
        return result;
    }

    protected MaritimeId setPosition(MaritimeId id, double lat, double lon) {
        locs.get(id).lat = lat;
        locs.get(id).lon = lon;
        return id;
    }

    protected PersistentConnection setPosition(PersistentConnection pnc, double lat, double lon) {
        locs.get(pnc.getLocalId()).lat = lat;
        locs.get(pnc.getLocalId()).lon = lon;
        return pnc;
    }

    int clientPort;

    @Before
    public void setup() throws Exception {
        clientPort = ThreadLocalRandom.current().nextInt(40000, 50000);
        if (useProxy) {
            si = new ENavNetworkServer(12222);
            pt = new ProxyTester(new InetSocketAddress(clientPort), new InetSocketAddress(12222));
            pt.start();
        } else {
            si = new ENavNetworkServer(clientPort);
        }
        si.start();
    }

    @After
    public void teardown() throws InterruptedException {
        for (final PersistentConnection c : clients.values()) {
            es.execute(new Runnable() {
                public void run() {
                    c.close();
                }
            });
        }
        for (PersistentConnection c : clients.values()) {
            assertTrue(c.awaitState(State.TERMINATED, 5, TimeUnit.SECONDS));
        }

        clients.clear();
        si.shutdown();
        es.shutdown();
        if (pt != null) {
            pt.shutdown();
        }
        assertTrue(es.awaitTermination(10, TimeUnit.SECONDS));

        assertTrue(si.awaitTerminated(10, TimeUnit.SECONDS));
    }

    static class LocationSup extends Supplier<PositionTime> {
        double lat = 0;
        double lon = 0;

        /** {@inheritDoc} */
        @Override
        public PositionTime get() {
            return PositionTime.create(lat, lon, System.currentTimeMillis());
        }

    }
}
