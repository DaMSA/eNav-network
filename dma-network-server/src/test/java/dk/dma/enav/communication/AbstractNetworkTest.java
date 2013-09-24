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
import dk.dma.enav.communication.MaritimeNetworkConnection.State;
import dk.dma.enav.model.MaritimeId;
import dk.dma.enav.model.geometry.PositionTime;
import dk.dma.enav.util.function.Supplier;
import dk.dma.navnet.server.EmbeddableCloudServer;

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

    protected final ConcurrentHashMap<MaritimeId, MaritimeNetworkConnection> clients = new ConcurrentHashMap<>();
    ExecutorService es = Executors.newCachedThreadPool();

    protected final ConcurrentHashMap<MaritimeId, LocationSup> locs = new ConcurrentHashMap<>();

    ProxyTester pt;

    EmbeddableCloudServer si;

    final boolean useProxy;

    public AbstractNetworkTest() {
        this(false);
    }

    public AbstractNetworkTest(boolean useProxy) {
        this.useProxy = useProxy;
    }

    protected MaritimeNetworkConnection newClient() throws Exception {
        for (;;) {
            MaritimeId id = MaritimeId.create("mmsi://" + ThreadLocalRandom.current().nextInt(1000));
            if (!clients.containsKey(id)) {
                MaritimeNetworkConnection pc = newClient(id);
                pc.connect();
            }
        }
    }

    protected MaritimeNetworkConnection newClient(double lat, double lon) throws Exception {
        for (;;) {
            MaritimeId id = MaritimeId.create("mmsi://" + ThreadLocalRandom.current().nextInt(1000));
            if (!clients.containsKey(id)) {
                return newClient(id, lat, lon);
            }
        }
    }

    protected MaritimeNetworkConnection newClient(MaritimeNetworkConnectionBuilder b) throws Exception {
        locs.put(b.getId(), new LocationSup());
        MaritimeNetworkConnection c = b.build();
        c.connect();
        clients.put(b.getId(), c);
        return c;
    }

    protected MaritimeNetworkConnection newClient(MaritimeId id) throws Exception {
        MaritimeNetworkConnectionBuilder b = newBuilder(id);
        locs.put(id, new LocationSup());
        MaritimeNetworkConnection c = b.build();
        c.connect();
        clients.put(id, c);
        return c;
    }

    protected MaritimeNetworkConnection newClient(MaritimeId id, double lat, double lon) throws Exception {
        MaritimeNetworkConnectionBuilder b = newBuilder(id);
        LocationSup ls = new LocationSup();
        b.setPositionSupplier(ls);
        locs.put(id, ls);
        setPosition(id, lat, lon);
        MaritimeNetworkConnection c = b.build();
        c.connect();
        clients.put(id, c);
        return c;
    }

    protected Future<MaritimeNetworkConnection> newClientAsync(final MaritimeId id) throws Exception {
        final MaritimeNetworkConnectionBuilder b = newBuilder(id);
        locs.put(id, new LocationSup());
        return es.submit(new Callable<MaritimeNetworkConnection>() {

            @Override
            public MaritimeNetworkConnection call() throws Exception {
                MaritimeNetworkConnection c = b.build();
                c.connect();
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

    protected MaritimeId setPosition(MaritimeId id, double lat, double lon) {
        locs.get(id).lat = lat;
        locs.get(id).lon = lon;
        return id;
    }

    protected MaritimeNetworkConnection setPosition(MaritimeNetworkConnection pnc, double lat, double lon) {
        locs.get(pnc.getLocalId()).lat = lat;
        locs.get(pnc.getLocalId()).lon = lon;
        return pnc;
    }

    int clientPort;

    @Before
    public void setup() throws Exception {
        clientPort = ThreadLocalRandom.current().nextInt(40000, 50000);
        if (useProxy) {
            si = new EmbeddableCloudServer(12222);
            pt = new ProxyTester(new InetSocketAddress(clientPort), new InetSocketAddress(12222));
            pt.start();
        } else {
            si = new EmbeddableCloudServer(clientPort);
        }
        si.start();
    }

    @After
    public void teardown() throws InterruptedException {
        for (final MaritimeNetworkConnection c : clients.values()) {
            es.execute(new Runnable() {
                public void run() {
                    c.close();
                }
            });
        }
        for (MaritimeNetworkConnection c : clients.values()) {
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
        double lat;
        double lon;

        /** {@inheritDoc} */
        @Override
        public PositionTime get() {
            return PositionTime.create(lat, lon, System.currentTimeMillis());
        }

    }
}
