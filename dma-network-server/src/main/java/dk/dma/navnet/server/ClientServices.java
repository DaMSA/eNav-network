package dk.dma.navnet.server;

import static java.util.Objects.requireNonNull;
import jsr166e.ConcurrentHashMapV8;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.dma.navnet.core.messages.s2c.service.RegisterService;

/**
 * Manages services for a single client.
 * 
 * @author Kasper Nielsen
 */
class ClientServices {

    /** A logger. */
    private static final Logger LOG = LoggerFactory.getLogger(ClientServices.class);

    /** The client */
    final ServerConnection holder;

    /** A map of all registered services. */
    final ConcurrentHashMapV8<String, String> services = new ConcurrentHashMapV8<>();

    ClientServices(ServerConnection ch) {
        this.holder = requireNonNull(ch);
    }

    void registerService(RegisterService s) {
        services.put(s.getServiceName(), s.getServiceName());
        LOG.debug("Registered remote service " + s.getServiceName() + "@" + holder.id);
        System.out.println("Registered remote service " + s.getServiceName() + "@" + holder.id);
    }

    boolean hasService(String name) {
        for (String c : services.keySet()) {
            if (c.equals(name)) {
                return true;
            }
        }
        return false;
    }
}
