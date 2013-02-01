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
package dk.dma.enav.network;

import static java.util.Objects.requireNonNull;
import dk.dma.enav.model.MaritimeId;
import dk.dma.enav.model.geometry.PositionTime;
import dk.dma.enav.net.MaritimeNetworkConnection;
import dk.dma.enav.network.client.ClientNetwork;
import dk.dma.enav.util.function.Supplier;

/**
 * 
 * @author Kasper Nielsen
 */
public class MaritimeNetworkConnectionBuilder {

    private final MaritimeId id;

    private String nodes = "localhost:11111";

    private Supplier<PositionTime> positionSupplier;

    MaritimeNetworkConnectionBuilder(MaritimeId id) {
        this.id = requireNonNull(id);
    }

    public MaritimeNetworkConnection connect() throws Exception {
        return ClientNetwork.connect(this);
    }

    /**
     * @return the id
     */
    public MaritimeId getId() {
        return id;
    }

    public String getNodes() {
        return nodes;
    }

    /**
     * @return the positionSupplier
     */
    public Supplier<PositionTime> getPositionSupplier() {
        return positionSupplier;
    }

    public MaritimeNetworkConnectionBuilder setBootstrapNodes(String nodes) {
        this.nodes = nodes;
        return this;
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