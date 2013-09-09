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

import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map.Entry;

import jsr166e.ConcurrentHashMapV8;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.dma.enav.model.geometry.Area;
import dk.dma.enav.model.geometry.Circle;
import dk.dma.enav.model.geometry.CoordinateSystem;
import dk.dma.enav.model.geometry.PositionTime;
import dk.dma.enav.util.function.BiConsumer;
import dk.dma.navnet.core.messages.s2c.service.FindService;
import dk.dma.navnet.core.messages.s2c.service.RegisterService;

/**
 * Manages services for a single connected client.
 * 
 * @author Kasper Nielsen
 */
class ServerConnectionServiceManager {

    /** A logger. */
    private static final Logger LOG = LoggerFactory.getLogger(ServerConnectionServiceManager.class);

    /** The client */
    final ServerConnection connection;

    /** A map of all registered services at the client. */
    final ConcurrentHashMapV8<String, String> services = new ConcurrentHashMapV8<>();

    ServerConnectionServiceManager(ServerConnection connection) {
        this.connection = requireNonNull(connection);
    }

    void registerService(RegisterService s) {
        LOG.debug("Registered remote service " + s.getServiceName() + "@" + connection.clientId);
        services.put(s.getServiceName(), s.getServiceName());
    }

    boolean hasService(String name) {
        for (String c : services.keySet()) {
            if (c.equals(name)) {
                return true;
            }
        }
        return false;
    }

    void findService(final FindService m) {
        final PositionTime pos = connection.latestPosition;
        double meters = m.getMeters() <= 0 ? Integer.MAX_VALUE : m.getMeters();
        Area a = new Circle(pos, meters, CoordinateSystem.GEODETIC);
        // Find all services with the area
        final ConcurrentHashMapV8<ServerConnection, PositionTime> map = new ConcurrentHashMapV8<>();
        connection.server.tracker.forEachWithinArea(a, new BiConsumer<Target, PositionTime>() {
            public void accept(Target target, PositionTime r) {
                ServerConnection l = (ServerConnection) target.getConnection();
                if (l != null && l.services.hasService(m.getServiceName())) {
                    map.put(l, r);
                }
            }
        });
        map.remove(connection);
        // Sort by distance
        List<Entry<ServerConnection, PositionTime>> l = new ArrayList<>(map.entrySet());
        Collections.sort(l, new Comparator<Entry<ServerConnection, PositionTime>>() {
            public int compare(Entry<ServerConnection, PositionTime> o1, Entry<ServerConnection, PositionTime> o2) {
                return Double.compare(o1.getValue().distanceTo(pos, CoordinateSystem.GEODETIC), o2.getValue()
                        .distanceTo(pos, CoordinateSystem.GEODETIC));
            }
        });

        // If we have a maximum number of results, filter the list
        if (l.size() > m.getMax()) {
            l = l.subList(0, m.getMax());
        }

        // Extract the maritime id
        List<String> list = new ArrayList<>();
        for (Entry<ServerConnection, PositionTime> e : l) {
            list.add(e.getKey().clientId.toString());
        }
        connection.sendConnectionMessage(m.createReply(list.toArray(new String[list.size()])));
    }
}
