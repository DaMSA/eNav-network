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
package dk.dma.navnet.server.rest;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

import dk.dma.commons.util.JSONObject;
import dk.dma.commons.web.rest.AbstractResource;
import dk.dma.navnet.server.EmbeddableCloudServer;

/**
 * Resources that query AisStore.
 * 
 * @author Kasper Nielsen
 */
@Path("/connections")
public class ConnectionResource extends AbstractResource {

    /**
     * Returns the number of connections as json.
     * 
     * @return the number of connections as json
     */
    @GET
    @Path("/numberOfConnections")
    public JSONObject getNumberOfConnections() {
        EmbeddableCloudServer s = get(EmbeddableCloudServer.class);
        return JSONObject.single("numberOfConnections", s.getNumberOfConnections());
    }

    // Get connections + position + type

    // Received messages: message content, messageid, recevied timestamp, ack by client (time, via message)
    // send message: message content, messageid, sent timestamp, ack by client (time, via message)
    // Get last xxx messages for client

    // Get registered services per client
    // Get registered services types
    // Get registered Services
    //
    //

    // both send received

}
