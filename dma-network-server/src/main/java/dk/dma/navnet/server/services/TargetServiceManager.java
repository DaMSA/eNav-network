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
import jsr166e.ConcurrentHashMapV8;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.dma.navnet.messages.s2c.service.RegisterService;
import dk.dma.navnet.server.targets.Target;

/**
 * Manages services for a single connected client.
 * 
 * @author Kasper Nielsen
 */
public class TargetServiceManager {

    /** A logger. */
    private static final Logger LOG = LoggerFactory.getLogger(TargetServiceManager.class);

    /** The client */
    final Target target;

    /** A map of all registered services at the client. */
    final ConcurrentHashMapV8<String, String> services = new ConcurrentHashMapV8<>();

    public TargetServiceManager(Target target) {
        this.target = requireNonNull(target);
    }

    public void registerService(RegisterService s) {
        LOG.debug("Registered remote service " + s.getServiceName() + "@" + target.getId());
        services.put(s.getServiceName(), s.getServiceName());
    }

    public boolean hasService(String name) {
        for (String c : services.keySet()) {
            if (c.equals(name)) {
                return true;
            }
        }
        return false;
    }
}
