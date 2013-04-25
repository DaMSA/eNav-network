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
package dk.dma.navnet.client;

import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import java.util.List;

import dk.dma.enav.communication.ConnectionListener;
import dk.dma.enav.communication.PersistentConnection;
import dk.dma.enav.model.MaritimeId;
import dk.dma.enav.model.geometry.PositionTime;
import dk.dma.enav.util.function.Consumer;
import dk.dma.enav.util.function.Supplier;

/**
 * 
 * @author Kasper Nielsen
 */
public class MaritimeNetworkConnectionBuilder {

    private final MaritimeId id;

    final List<ConnectionListener> listeners = new ArrayList<>();

    private String nodes = "localhost:43234";

    private Supplier<PositionTime> positionSupplier = new Supplier<PositionTime>() {
        public PositionTime get() {
            return PositionTime.create(0, 0, 0);
        }
    };

    MaritimeNetworkConnectionBuilder(MaritimeId id) {
        this.id = requireNonNull(id);
    }

    /**
     * Adds a state listener that will be invoked whenever the state of the connection changes.
     * 
     * @param stateListener
     *            the state listener
     * @throws NullPointerException
     *             if the specified listener is null
     * @see #removeStateListener(Consumer)
     */
    public MaritimeNetworkConnectionBuilder addListener(ConnectionListener listener) {
        listeners.add(requireNonNull(listener, "listener is null"));
        return this;
    }

    public PersistentConnection build() throws Exception {
        DefaultPersistentConnection con = new DefaultPersistentConnection(this);
        con.start();
        return con;
    }

    public String getHost() {
        return nodes;
    }

    /**
     * @return the id
     */
    public MaritimeId getId() {
        return id;
    }

    /**
     * @return the positionSupplier
     */
    public Supplier<PositionTime> getPositionSupplier() {
        return positionSupplier;
    }

    public MaritimeNetworkConnectionBuilder setHost(String host) {
        this.nodes = requireNonNull(host);
        return this;
    }

    public MaritimeNetworkConnectionBuilder setPositionSupplier(Supplier<PositionTime> positionSupplier) {
        this.positionSupplier = requireNonNull(positionSupplier);
        return this;
    }

    public static MaritimeNetworkConnectionBuilder create(MaritimeId id) {
        return new MaritimeNetworkConnectionBuilder(id);
    }

    public static MaritimeNetworkConnectionBuilder create(String id) {
        return new MaritimeNetworkConnectionBuilder(MaritimeId.create(id));
    }
}
