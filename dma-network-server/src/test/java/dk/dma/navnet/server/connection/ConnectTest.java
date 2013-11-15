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
package dk.dma.navnet.server.connection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import dk.dma.navnet.messages.auxiliary.ConnectedMessage;
import dk.dma.navnet.messages.auxiliary.HelloMessage;
import dk.dma.navnet.messages.auxiliary.WelcomeMessage;
import dk.dma.navnet.server.AbstractServerConnectionTest;
import dk.dma.navnet.server.TesstEndpoint;


/**
 * 
 * @author Kasper Nielsen
 */
public class ConnectTest extends AbstractServerConnectionTest {

    @Test
    public void connectTest() throws Exception {
        TesstEndpoint t = newClient();
        WelcomeMessage wm = t.take(WelcomeMessage.class);
        assertNotNull(wm.getServerId());
        t.send(new HelloMessage(ID2, "foo", "", 0, 1, 1));

        ConnectedMessage cm = t.take(ConnectedMessage.class);
        assertEquals(0, cm.getLastReceivedMessageId());
    }
}
