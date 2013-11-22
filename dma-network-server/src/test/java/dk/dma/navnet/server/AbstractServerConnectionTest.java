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
package dk.dma.navnet.server;

import static org.junit.Assert.assertTrue;

import java.net.URI;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import javax.websocket.ContainerProvider;
import javax.websocket.WebSocketContainer;

import org.junit.After;
import org.junit.Before;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import dk.dma.enav.maritimecloud.MaritimeCloudClientConfiguration;
import dk.dma.enav.model.MaritimeId;
import dk.dma.navnet.client.TestWebSocketServer;
import dk.dma.navnet.messages.auxiliary.ConnectedMessage;
import dk.dma.navnet.messages.auxiliary.HelloMessage;
import dk.dma.navnet.messages.auxiliary.WelcomeMessage;

/**
 * 
 * @author Kasper Nielsen
 */
public abstract class AbstractServerConnectionTest {

    public static final MaritimeId ID1 = MaritimeId.create("mmsi://1");

    public static final MaritimeId ID2 = MaritimeId.create("mmsi://2");

    public static final MaritimeId ID3 = MaritimeId.create("mmsi://3");

    public static final MaritimeId ID4 = MaritimeId.create("mmsi://4");

    public static final MaritimeId ID5 = MaritimeId.create("mmsi://5");

    public static final MaritimeId ID6 = MaritimeId.create("mmsi://6");

    private CopyOnWriteArraySet<TesstEndpoint> allClient = new CopyOnWriteArraySet<>();

    int clientPort;

    MaritimeCloudClientConfiguration conf;

    InternalServer server;

    TestWebSocketServer ws;

    @After
    public void after() throws Exception {
        server.shutdown();
        assertTrue(server.awaitTerminated(5, TimeUnit.SECONDS));
        for (TesstEndpoint te : allClient) {
            te.close();
        }
    }

    @Before
    public void before() throws Exception {
        clientPort = ThreadLocalRandom.current().nextInt(40000, 50000);
        ServerConfiguration sc = new ServerConfiguration();
        sc.setServerPort(clientPort);
        server = new InternalServer(sc);
        server.start();
    }

    protected TesstEndpoint newClient() throws Exception {
        WebSocketContainer container = ContainerProvider.getWebSocketContainer();
        TesstEndpoint t = new TesstEndpoint();
        container.connectToServer(t, new URI("ws://localhost:" + clientPort));
        allClient.add(t);
        return t;
    }

    protected TesstEndpoint newClient(MaritimeId id) throws Exception {
        return newClient(id, 1, 1);
    }

    protected TesstEndpoint newClient(MaritimeId id, double latitude, double longitude) throws Exception {
        TesstEndpoint t = newClient();
        t.take(WelcomeMessage.class);
        t.send(new HelloMessage(id, "foo", "", 0, latitude, longitude));
        t.take(ConnectedMessage.class);
        return t;
    }


    protected static String escape(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
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

    protected static String persistAndEscape(Object o) {
        return escape(persist(o));
    }

}
