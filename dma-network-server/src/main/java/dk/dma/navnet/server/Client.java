/*
[ * Copyright (c) 2008 Kasper Nielsen.
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
import dk.dma.enav.model.MaritimeId;
import dk.dma.enav.model.geometry.PositionTime;

/**
 * 
 * @author Kasper Nielsen
 */
class Client {

    final MaritimeId id;

    volatile ServerTransport sh;

    final ENavNetworkServer server;

    final ClientServices services;
    final ConnectionManager cm;
    /** The latest position of the client. */
    volatile PositionTime latestPosition;

    /**
     * @param currentConnection
     * @param server
     */
    Client(MaritimeId id, ENavNetworkServer server, ServerTransport currentConnection) {
        this.id = id;
        this.sh = requireNonNull(currentConnection);
        this.server = requireNonNull(server);
        this.cm = requireNonNull(server.at);
        services = new ClientServices(this);
    }

    enum State {
        CONNECTED, CREATED, RECONNECTING, DEAD;
    }
}
