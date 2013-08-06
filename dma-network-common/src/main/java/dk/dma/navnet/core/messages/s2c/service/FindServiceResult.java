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
package dk.dma.navnet.core.messages.s2c.service;

import static java.util.Objects.requireNonNull;

import java.io.IOException;

import dk.dma.navnet.core.messages.MessageType;
import dk.dma.navnet.core.messages.s2c.ServerResponseMessage;
import dk.dma.navnet.core.messages.util.TextMessageReader;
import dk.dma.navnet.core.messages.util.TextMessageWriter;

/**
 * 
 * @author Kasper Nielsen
 */
public class FindServiceResult extends ServerResponseMessage {

    final String[] maritimeIds;

    public FindServiceResult(TextMessageReader pr) throws IOException {
        super(MessageType.FIND_SERVICE_ACK, pr);
        this.maritimeIds = requireNonNull(pr.takeStringArray());
    }

    /**
     * @param messageType
     */
    public FindServiceResult(long id, String[] ids) {
        super(MessageType.FIND_SERVICE_ACK, id);
        this.maritimeIds = requireNonNull(ids);

    }

    public String[] getMax() {
        return maritimeIds;
    }

    public final Class<String[]> getType() {
        return String[].class;
    }

    /** {@inheritDoc} */
    @Override
    protected void write1(TextMessageWriter w) {
        w.writeStringArray(maritimeIds);
    }
}
