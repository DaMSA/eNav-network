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

import jsr166e.ConcurrentHashMapV8;
import dk.dma.navnet.core.messages.auxiliary.ConnectedMessage;
import dk.dma.navnet.core.messages.auxiliary.HelloMessage;
import dk.dma.navnet.core.messages.auxiliary.PositionReportMessage;
import dk.dma.navnet.core.messages.auxiliary.WelcomeMessage;
import dk.dma.navnet.core.messages.c2c.AbstractRelayedMessage;
import dk.dma.navnet.core.messages.c2c.broadcast.BroadcastMsg;
import dk.dma.navnet.core.messages.s2c.service.FindService;
import dk.dma.navnet.core.messages.s2c.service.RegisterService;
import dk.dma.navnet.core.spi.AbstractServerHandler;

/**
 * 
 * @author Kasper Nielsen
 */
class ServerHandler extends AbstractServerHandler {

    final ConnectionManager cm;

    volatile Client holder;

    long nextReplyId;

    State state = State.CREATED;

    ServerHandler(ConnectionManager cm) {
        this.cm = requireNonNull(cm);
    }

    /** {@inheritDoc} */
    @Override
    public void broadcast(BroadcastMsg m) {
        cm.broadcast(this, m);
    }

    /** {@inheritDoc} */
    @Override
    protected void closed(int statusCode, String reason) {
        cm.server.tracker.remove(this.holder);
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
    public void findService(final FindService m) {
        List<String> list = new ArrayList<>();
        final ConcurrentHashMapV8<Client, String> map = new ConcurrentHashMapV8<>();
        cm.connections.forEachValueInParallel(new ConcurrentHashMapV8.Action<Client>() {
            public void apply(Client r) {
                if (r.services.hasService(m.getServiceName())) {
                    map.put(r, "");
                }
            }
        });
        for (Client ch : map.keySet()) {
            list.add(ch.id.toString());
        }
        sendMessage(m.createReply(list.toArray(new String[list.size()])));
    }

    /** {@inheritDoc} */
    @Override
    public void hello(HelloMessage m) {
        UUID uuid = UUID.randomUUID();
        cm.addConnection(m.getClientId(), m.getClientId().toString(), this);
        sendMessage(new ConnectedMessage(uuid.toString()));
    }

    /** {@inheritDoc} */
    @Override
    public void onError(Throwable cause) {
        cm.server.tracker.remove(this.holder);
        cm.server.at.disconnected(this);
    }

    /** {@inheritDoc} */
    @Override
    public void positionReport(PositionReportMessage m) {
        cm.server.tracker.update(this.holder, m.getPositionTime());
    }

    /** {@inheritDoc} */
    @Override
    public void registerService(RegisterService m) {
        holder.services.registerService(m);
        sendMessage(m.createReply());
    }

    /** {@inheritDoc} */
    @Override
    public void relay(AbstractRelayedMessage m) {
        String d = m.getDestination();
        ServerHandler c = cm.getConnection(d);
        if (c == null) {
            System.err.println("Unknown destination " + d);
            System.err.println("Available " + cm.getAllConnectionIds());
        } else {
            c.sendRawTextMessage(m.getReceivedRawMesage());
        }
    }

    enum State {
        CONNECTED, CREATED, DISCONNECTED
    }
}
