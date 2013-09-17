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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

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

    long heartbeatInterval = TimeUnit.SECONDS.toNanos(3);

    private Supplier<PositionTime> positionSupplier = new Supplier<PositionTime>() {
        public PositionTime get() {
            return PositionTime.create(0, 0, 0);
        }
    };

    MaritimeNetworkConnectionBuilder(MaritimeId id) {
        this.id = requireNonNull(id);
    }

    public MaritimeNetworkConnectionBuilder setHeartbeatInterval(long interval, TimeUnit intervalUnit) {
        this.heartbeatInterval = intervalUnit.toNanos(interval);
        return this;
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
