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
import dk.dma.enav.model.MaritimeId;
import dk.dma.enav.model.geometry.PositionTime;
import dk.dma.enav.util.function.Supplier;
import dk.dma.navnet.server.InternalServer;
import dk.dma.navnet.server.ServerConfiguration;

/**
 * 
 * @author Kasper Nielsen
 */
public abstract class AbstractNetworkTest {

    public static final MaritimeId ID1 = MaritimeId.create("mmsi://1");

    public static final MaritimeId ID2 = MaritimeId.create("mmsi://2");

    public static final MaritimeId ID3 = MaritimeId.create("mmsi://3");

    public static final MaritimeId ID4 = MaritimeId.create("mmsi://4");

    public static final MaritimeId ID5 = MaritimeId.create("mmsi://5");

    public static final MaritimeId ID6 = MaritimeId.create("mmsi://6");

    int clientPort;

    protected final ConcurrentHashMap<MaritimeId, MaritimeNetworkClient> clients = new ConcurrentHashMap<>();

    ExecutorService es = Executors.newCachedThreadPool();

    protected final ConcurrentHashMap<MaritimeId, LocationSup> locs = new ConcurrentHashMap<>();

    ProxyTester pt;

    InternalServer si;

    final boolean useProxy;

    public AbstractNetworkTest() {
        this(false);
    }

    public AbstractNetworkTest(boolean useProxy) {
        this.useProxy = useProxy;
    }

    protected MaritimeNetworkClientConfiguration newBuilder(MaritimeId id) {
        MaritimeNetworkClientConfiguration b = MaritimeNetworkClientConfiguration.create(id);
        b.setHost("localhost:" + clientPort);
        return b;
    }

    protected MaritimeNetworkClient newClient() throws Exception {
        for (;;) {
            MaritimeId id = MaritimeId.create("mmsi://" + ThreadLocalRandom.current().nextInt(1000));
            if (!clients.containsKey(id)) {
                return newClient(id);
            }
        }
    }

    protected MaritimeNetworkClient newClient(double lat, double lon) throws Exception {
        for (;;) {
            MaritimeId id = MaritimeId.create("mmsi://" + ThreadLocalRandom.current().nextInt(1000));
            if (!clients.containsKey(id)) {
                return newClient(id, lat, lon);
            }
        }
    }

    protected MaritimeNetworkClient newClient(MaritimeId id) throws Exception {
        MaritimeNetworkClientConfiguration b = newBuilder(id);
        locs.put(id, new LocationSup());
        MaritimeNetworkClient c = b.build();
        clients.put(id, c);
        return c;
    }

    protected MaritimeNetworkClient newClient(MaritimeId id, double lat, double lon) throws Exception {
        MaritimeNetworkClientConfiguration b = newBuilder(id);
        LocationSup ls = new LocationSup();
        b.setPositionSupplier(ls);
        locs.put(id, ls);
        setPosition(id, lat, lon);
        MaritimeNetworkClient c = b.build();
        clients.put(id, c);
        return c;
    }

    protected MaritimeNetworkClient newClient(MaritimeNetworkClientConfiguration b) throws Exception {
        locs.put(b.getId(), new LocationSup());
        MaritimeNetworkClient c = b.build();
        clients.put(b.getId(), c);
        return c;
    }

    protected Future<MaritimeNetworkClient> newClientAsync(final MaritimeId id) throws Exception {
        final MaritimeNetworkClientConfiguration b = newBuilder(id);
        locs.put(id, new LocationSup());
        return es.submit(new Callable<MaritimeNetworkClient>() {

            @Override
            public MaritimeNetworkClient call() throws Exception {
                MaritimeNetworkClient c = b.build();
                clients.put(id, c);
                return c;
            }
        });
    }

    protected Set<MaritimeNetworkClient> newClients(int count) throws Exception {
        HashSet<Future<MaritimeNetworkClient>> futures = new HashSet<>();
        for (int j = 0; j < count; j++) {
            futures.add(newClientAsync(MaritimeId.create("mmsi://1234" + j)));
        }
        HashSet<MaritimeNetworkClient> result = new HashSet<>();
        for (Future<MaritimeNetworkClient> f : futures) {
            result.add(f.get(3, TimeUnit.SECONDS));
        }
        return result;
    }

    protected MaritimeId setPosition(MaritimeId id, double lat, double lon) {
        locs.get(id).lat = lat;
        locs.get(id).lon = lon;
        return id;
    }

    protected MaritimeNetworkClient setPosition(MaritimeNetworkClient pnc, double lat, double lon) {
        locs.get(pnc.getClientId()).lat = lat;
        locs.get(pnc.getClientId()).lon = lon;
        return pnc;
    }

    @Before
    public void setup() throws Exception {
        clientPort = ThreadLocalRandom.current().nextInt(40000, 50000);
        ServerConfiguration sc = new ServerConfiguration();
        if (useProxy) {
            sc.setServerPort(12222);
            si = new InternalServer(sc);
            pt = new ProxyTester(new InetSocketAddress(clientPort), new InetSocketAddress(12222));
            pt.start();
        } else {
            sc.setServerPort(clientPort);
            si = new InternalServer(sc);
        }
        si.start();
    }

    @After
    public void teardown() throws InterruptedException {
        for (final MaritimeNetworkClient c : clients.values()) {
            es.execute(new Runnable() {
                public void run() {
                    c.close();
                }
            });
        }
        for (MaritimeNetworkClient c : clients.values()) {
            assertTrue(c.awaitTermination(2, TimeUnit.SECONDS));
        }

        clients.clear();
        si.shutdown();
        es.shutdown();
        if (pt != null) {
            pt.shutdown();
        }
        assertTrue(es.awaitTermination(10, TimeUnit.SECONDS));

        assertTrue(si.awaitTerminated(10, TimeUnit.SECONDS));
        System.out.println("bye");
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
