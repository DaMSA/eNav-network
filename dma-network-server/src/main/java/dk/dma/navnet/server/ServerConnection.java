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
package dk.dma.navnet.server;

import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import dk.dma.enav.model.MaritimeId;
import dk.dma.navnet.core.messages.c2c.AbstractRelayedMessage;
import dk.dma.navnet.core.messages.c2c.Broadcast;
import dk.dma.navnet.core.messages.s2c.PositionReportMessage;
import dk.dma.navnet.core.messages.s2c.connection.ConnectedMessage;
import dk.dma.navnet.core.messages.s2c.connection.HelloMessage;
import dk.dma.navnet.core.messages.s2c.connection.WelcomeMessage;
import dk.dma.navnet.core.messages.s2c.service.FindServices;
import dk.dma.navnet.core.messages.s2c.service.RegisterService;
import dk.dma.navnet.core.spi.ServerHandler;

/**
 * 
 * @author Kasper Nielsen
 */
public class ServerConnection extends ServerHandler {

    volatile MaritimeId clientId;

    final ConnectionManager cm;

    long nextReplyId;

    State state = State.CREATED;

    /** {@inheritDoc} */
    @Override
    public void onError(Throwable cause) {
        cm.server.tracker.remove(this);
        cm.server.at.disconnected(this);
    }

    ServerConnection(ConnectionManager cm) {
        this.cm = requireNonNull(cm);
    }

    /** {@inheritDoc} */
    @Override
    public void broadcast(String msg, Broadcast m) {
        cm.broadcast(this, msg, m);
    }

    /** {@inheritDoc} */
    @Override
    protected void closed(int statusCode, String reason) {
        cm.server.tracker.remove(this);
        cm.server.at.disconnected(this);
        super.closed(statusCode, reason);
    }

    /** {@inheritDoc} */
    @Override
    public void connected() {
        sendMessage(new WelcomeMessage(1, cm.server.id, "enavServer/1.0"));
    }

    /** {@inheritDoc} */
    @Override
    public void findService(FindServices m) {
        List<String> list = new ArrayList<>();
        for (MaritimeId id : cm.server.registeredServices.findServicesOfType(m.getServiceName()).keySet()) {
            list.add(id.toString());
        }
        sendMessage(m.createReply(list.toArray(new String[list.size()])));
    }

    /** {@inheritDoc} */
    @Override
    public void hello(HelloMessage m) {
        UUID uuid = UUID.randomUUID();
        clientId = m.getClientId();
        cm.addConnection(m.getClientId().toString(), this);
        sendMessage(new ConnectedMessage(uuid.toString()));
    }

    /** {@inheritDoc} */
    @Override
    public void positionReport(PositionReportMessage m) {
        cm.server.tracker.update(this, m.getPositionTime());
    }

    /** {@inheritDoc} */
    @Override
    public void registerService(RegisterService m) {
        cm.server.registeredServices.registerService(clientId, m);
        sendMessage(m.createReply());
    }

    /** {@inheritDoc} */
    @Override
    public void relay(String raw, AbstractRelayedMessage m) {
        String d = m.getDestination();
        ServerConnection c = cm.getConnection(d);
        if (c == null) {
            System.err.println("Unknown destination " + d);
            System.err.println("Available " + cm.getAllConnectionIds());
        } else {
            c.sendRawTextMessage(raw);
        }
    }

    enum State {
        CONNECTED, CREATED, DISCONNECTED
    }
}
