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

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import javax.websocket.CloseReason;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.RemoteEndpoint.Basic;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

import dk.dma.enav.model.shore.ServerId;
import dk.dma.navnet.messages.TransportMessage;
import dk.dma.navnet.messages.auxiliary.WelcomeMessage;

/**
 * 
 * @author Kasper Nielsen
 */
@ServerEndpoint(value = "/")
public class TestClientEndpoint {

    public BlockingQueue<TransportMessage> m = new ArrayBlockingQueue<>(10000);

    Session session;

    public void close() throws IOException {
        session.close(new CloseReason(CloseReason.CloseCodes.UNEXPECTED_CONDITION, "TestClientEndpoint.close()"));
    }

    @OnMessage
    public final void messageReceived(String msg, Session userSession) throws InterruptedException, IOException {
        if (session != userSession) {
            throw new Error();
        }
        TransportMessage tm = TransportMessage.parseMessage(msg);
        m.put(tm);
    }

    @OnOpen
    public final void onWebsocketOpen(Session session) {
        this.session = session;
        m.clear();
        send(new WelcomeMessage(1, new ServerId(123), "enavServer/1.0"));

    }

    protected <T extends TransportMessage> T poll(Class<T> c) {
        return c.cast(m.poll());
    }

    public void send(TransportMessage m) {
        Basic r = session.getBasicRemote();
        try {
            r.sendText(m.toJSON());
        } catch (IOException e) {
            throw new AssertionError(e);
        }
    }

    public <T extends BlockingQueue<TransportMessage>> T setQueue(T q) {
        this.m = requireNonNull(q);
        return q;
    }

    public <T extends TransportMessage> T take(Class<T> c) throws InterruptedException {
        return requireNonNull(c.cast(m.poll(5, TimeUnit.SECONDS)));
    }
}
