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
package dk.dma.enav.network.server;

import static java.util.Objects.requireNonNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import jsr166e.ConcurrentHashMapV8;
import jsr166e.ConcurrentHashMapV8.Fun;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.dma.enav.model.MaritimeId;
import dk.dma.enav.service.spi.MaritimeService;

/**
 * User by a server to hold all services that are available from clients connected to this host.
 * 
 * @author Kasper Nielsen
 */
class ServiceManager {

    /** A logger. */
    private static final Logger LOG = LoggerFactory.getLogger(ServiceManager.class);

    /** A map of all service registrations. */
    private final ConcurrentHashMapV8<MaritimeId, Registration> registrations = new ConcurrentHashMapV8<>();

    /**
     * Finds all services of the specified type.
     * 
     * @param serviceType
     *            the type of service
     */
    Map<MaritimeId, Class<? extends MaritimeService>> findServicesOfType(
            final Class<? extends MaritimeService> serviceType) {
        final ConcurrentHashMapV8<MaritimeId, Class<? extends MaritimeService>> m = new ConcurrentHashMapV8<>();
        registrations.forEachValueInParallel(new ConcurrentHashMapV8.Action<Registration>() {
            @Override
            public void apply(Registration r) {
                for (Class<? extends MaritimeService> c : r.services.keySet()) {
                    if (c.equals(serviceType)) {
                        m.put(r.id, c);
                    }
                }
            }
        });
        return new HashMap<>(m);
    }

    /**
     * Registers the specified service.
     * 
     * @param owner
     *            the id of the owner
     * @param service
     *            the service
     */
    void registerService(MaritimeId owner, MaritimeService service) {
        Class<? extends MaritimeService> serviceType = service.getClass();
        Registration r = registrations.computeIfAbsent(owner, new Fun<MaritimeId, Registration>() {
            public Registration apply(MaritimeId id) {
                return new Registration(id);
            }
        });
        r.services.put(serviceType, service);
        LOG.debug("Registered remote service " + serviceType.getCanonicalName() + "@" + owner);
    }

    void remove(MaritimeId id) {
        Registration remove = registrations.remove(id);
        if (remove != null) {
            for (Entry<Class<? extends MaritimeService>, MaritimeService> e : remove.services.entrySet()) {
                LOG.debug("Unregistering remote service " + e.getKey().getCanonicalName() + "@" + id);
            }
        }
    }

    static class Registration {

        /** The id of this registration. */
        final MaritimeId id;

        /** A map of all registered services. */
        final ConcurrentHashMapV8<Class<? extends MaritimeService>, MaritimeService> services = new ConcurrentHashMapV8<>();

        Registration(MaritimeId id) {
            this.id = requireNonNull(id);
        }
    }
}
