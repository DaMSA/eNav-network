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
package dk.dma.enav.communication.server;

import static org.junit.Assert.assertTrue;

import java.util.concurrent.TimeUnit;

import org.junit.Test;

import dk.dma.navnet.server.EmbeddableCloudServer;

/**
 * 
 * @author Kasper Nielsen
 */
public class StartStopServerTest {

    @Test
    public void noStart() throws InterruptedException {
        EmbeddableCloudServer s = new EmbeddableCloudServer(12345);

        s.shutdown();
        assertTrue(s.awaitTerminated(10, TimeUnit.SECONDS));
    }

    @Test
    public void start() throws Exception {
        EmbeddableCloudServer s = new EmbeddableCloudServer(12345);

        s.start();

        s.shutdown();
        assertTrue(s.awaitTerminated(10, TimeUnit.SECONDS));
    }

    @Test
    public void start2() throws Exception {
        EmbeddableCloudServer s = new EmbeddableCloudServer(12345);

        s.start();

        s.shutdown();
        assertTrue(s.awaitTerminated(10, TimeUnit.SECONDS));
    }
}
