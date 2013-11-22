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
package dk.dma.navnet.server.services;

import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map.Entry;

import jsr166e.ConcurrentHashMapV8;

import org.picocontainer.Startable;

import dk.dma.enav.model.geometry.Area;
import dk.dma.enav.model.geometry.Circle;
import dk.dma.enav.model.geometry.CoordinateSystem;
import dk.dma.enav.model.geometry.PositionTime;
import dk.dma.enav.util.function.BiConsumer;
import dk.dma.navnet.messages.s2c.service.FindService;
import dk.dma.navnet.messages.s2c.service.FindServiceResult;
import dk.dma.navnet.messages.s2c.service.RegisterService;
import dk.dma.navnet.messages.s2c.service.RegisterServiceResult;
import dk.dma.navnet.server.connection.ServerConnection;
import dk.dma.navnet.server.requests.RequestException;
import dk.dma.navnet.server.requests.RequestProcessor;
import dk.dma.navnet.server.requests.ServerMessageBus;
import dk.dma.navnet.server.target.Target;
import dk.dma.navnet.server.target.TargetManager;

/**
 * Manages services for all connected targets.
 * 
 * @author Kasper Nielsen
 */
public class ServiceManager implements Startable {

    final TargetManager tracker;

    private final ServerMessageBus bus;

    public ServiceManager(TargetManager tm, ServerMessageBus bus) {
        this.tracker = requireNonNull(tm);
        this.bus = requireNonNull(bus);
    }

    /** {@inheritDoc} */
    @Override
    public void start() {
        bus.subscribe(RegisterService.class, new RequestProcessor<RegisterService, RegisterServiceResult>() {
            @Override
            public RegisterServiceResult process(ServerConnection connection, RegisterService message)
                    throws RequestException {
                TargetServiceManager services = connection.getTarget().getServices();
                services.registerService(message);
                return message.createReply();
            }
        });

        bus.subscribe(FindService.class, new RequestProcessor<FindService, FindServiceResult>() {
            @Override
            public FindServiceResult process(ServerConnection connection, FindService r) throws RequestException {
                List<Entry<Target, PositionTime>> findService = findService(connection.getTarget(), r);
                List<String> list = new ArrayList<>();
                for (Entry<Target, PositionTime> e : findService) {
                    list.add(e.getKey().getId().toString());
                }
                return r.createReply(list.toArray(new String[list.size()]));
            }
        });
    }

    /**
     * Finds services in proximity to the specified target.
     * 
     * @param target
     *            the target that is trying to find the service
     * @param request
     *            the find service request
     * @return a sorted list of the targets that was found sorted by distance to the target doing the search
     */
    public List<Entry<Target, PositionTime>> findService(Target target, final FindService request) {
        final PositionTime pos = target.getLatestPosition();
        double meters = request.getMeters() <= 0 ? Integer.MAX_VALUE : request.getMeters();
        Area a = new Circle(pos, meters, CoordinateSystem.GEODETIC);
        // Find all services with the area
        final ConcurrentHashMapV8<Target, PositionTime> map = new ConcurrentHashMapV8<>();
        tracker.forEachWithinArea(a, new BiConsumer<Target, PositionTime>() {
            public void accept(Target target, PositionTime r) {

                if (target.getServices().hasService(request.getServiceName())) {
                    map.put(target, r);
                }
            }
        });
        // We remove ourself
        map.remove(target);

        // Sort by distance
        List<Entry<Target, PositionTime>> l = new ArrayList<>(map.entrySet());
        Collections.sort(l, new Comparator<Entry<Target, PositionTime>>() {
            public int compare(Entry<Target, PositionTime> o1, Entry<Target, PositionTime> o2) {
                return Double.compare(o1.getValue().distanceTo(pos, CoordinateSystem.GEODETIC), o2.getValue()
                        .distanceTo(pos, CoordinateSystem.GEODETIC));
            }
        });

        // If we have a maximum number of results, filter the list
        if (l.size() > request.getMax()) {
            l = l.subList(0, request.getMax());
        }

        return l;
    }

    /** {@inheritDoc} */
    @Override
    public void stop() {}
}
