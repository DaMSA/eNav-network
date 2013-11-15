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
package dk.dma.navnet.server.broadcast;

import static java.util.Objects.requireNonNull;
import jsr166e.CompletableFuture;
import jsr166e.CompletableFuture.Action;

import org.picocontainer.Startable;

import dk.dma.enav.model.MaritimeId;
import dk.dma.enav.model.geometry.PositionTime;
import dk.dma.navnet.messages.c2c.broadcast.BroadcastAck;
import dk.dma.navnet.messages.c2c.broadcast.BroadcastDeliver;
import dk.dma.navnet.messages.c2c.broadcast.BroadcastSend;
import dk.dma.navnet.messages.c2c.broadcast.BroadcastSendAck;
import dk.dma.navnet.server.connection.RequestException;
import dk.dma.navnet.server.connection.RequestProcessor;
import dk.dma.navnet.server.connection.ServerConnection;
import dk.dma.navnet.server.connection.ServerMessageBus;
import dk.dma.navnet.server.target.Target;
import dk.dma.navnet.server.target.TargetManager;

/**
 * 
 * @author Kasper Nielsen
 */
public class BroadcastManager implements Startable {
    private final TargetManager tm;

    private final ServerMessageBus bus;

    public BroadcastManager(TargetManager tm, ServerMessageBus bus) {
        this.tm = requireNonNull(tm);
        this.bus = requireNonNull(bus);
    }

    BroadcastSendAck broadcast(final ServerConnection source, final BroadcastSend send) {
        BroadcastDeliver bd = null;
        try {
            bd = BroadcastDeliver.create(MaritimeId.create("mmsi://2"), send.getPositionTime(), send.tryRead());
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        PositionTime sourcePositionTime = send.getPositionTime();
        for (Target t : tm) {
            final ServerConnection connection = t.getConnection();
            if (connection != null && source != connection) {
                PositionTime latest = t.getLatestPosition();
                if (latest != null) {
                    double distance = sourcePositionTime.geodesicDistanceTo(latest);
                    if (distance < send.getDistance()) {
                        CompletableFuture<Void> f = bus.sendConnectionMessage(connection, bd);
                        if (send.isReceiverAck()) {
                            f.thenAccept(new Action<Void>() {
                                public void accept(Void paramA) {
                                    Target t = connection.getTarget();
                                    BroadcastAck ba = new BroadcastAck(send.getReplyTo(), t.getId(), t
                                            .getLatestPosition());
                                    source.messageSend(ba);
                                }
                            });
                        }
                    }
                }
            }
        }
        return send.createReply();
    }

    /** {@inheritDoc} */
    @Override
    public void start() {
        bus.subscribe(BroadcastSend.class, new RequestProcessor<BroadcastSend, BroadcastSendAck>() {
            public BroadcastSendAck process(ServerConnection connection, BroadcastSend message) throws RequestException {
                return broadcast(connection, message);
            }
        });
    }

    /** {@inheritDoc} */
    @Override
    public void stop() {}
}
