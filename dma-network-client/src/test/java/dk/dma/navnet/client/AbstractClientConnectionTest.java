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
package dk.dma.navnet.client;

import static org.junit.Assert.assertTrue;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import dk.dma.enav.communication.MaritimeNetworkClient;
import dk.dma.enav.communication.MaritimeNetworkClientConfiguration;
import dk.dma.enav.model.MaritimeId;
import dk.dma.navnet.messages.auxiliary.ConnectedMessage;

/**
 * 
 * @author Kasper Nielsen
 */
public class AbstractClientConnectionTest {
    public static final MaritimeId ID1 = MaritimeId.create("mmsi://1");

    public static final MaritimeId ID2 = MaritimeId.create("mmsi://2");

    public static final MaritimeId ID3 = MaritimeId.create("mmsi://3");

    public static final MaritimeId ID4 = MaritimeId.create("mmsi://4");

    public static final MaritimeId ID5 = MaritimeId.create("mmsi://5");

    public static final MaritimeId ID6 = MaritimeId.create("mmsi://6");


    protected TestClientEndpoint t;

    int clientPort;

    MaritimeNetworkClientConfiguration conf;

    TestWebSocketServer ws;

    MaritimeNetworkClient client;

    @Before
    public void before() {
        clientPort = ThreadLocalRandom.current().nextInt(40000, 50000);
        ws = new TestWebSocketServer(clientPort);
        ws.start();
        t = ws.addEndpoint(new TestClientEndpoint());
        conf = MaritimeNetworkClientConfiguration.create(ID1);
        conf.setHost("localhost:" + clientPort);
        conf.setKeepAlive(1, TimeUnit.HOURS);
    }

    protected MaritimeNetworkClient create() {
        MaritimeNetworkClient c = conf.build();
        return client = c;
    }

    protected MaritimeNetworkClient createConnect() throws InterruptedException {
        MaritimeNetworkClient c = conf.build();
        t.m.take();
        t.send(new ConnectedMessage("ABC", 0));
        assertTrue(c.connection().awaitConnected(1, TimeUnit.SECONDS));
        assertTrue(c.connection().isConnected());
        return client = c;
    }


    @After
    public void after() throws Exception {
        if (client != null) {
            client.close();
            assertTrue(client.awaitTermination(5, TimeUnit.SECONDS));
        }
        ws.stop();
    }


    protected static String persist(Object o) {
        ObjectMapper om = new ObjectMapper();
        om.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
        try {
            return om.writeValueAsString(o);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Could not be persisted", e);
        }
    }

    protected static String escape(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    protected static String persistAndEscape(Object o) {
        return escape(persist(o));
    }

}
