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

import java.util.concurrent.TimeUnit;

import com.beust.jcommander.Parameter;
import com.google.inject.Injector;

import dk.dma.commons.app.AbstractCommandLineTool;

/**
 * Used to start a server from the command line.
 * 
 * @author Kasper Nielsen
 */
public class Main extends AbstractCommandLineTool {

    @Parameter(names = "-port", description = "The port to listen on")
    int port = EmbeddableCloudServer.DEFAULT_PORT;

    volatile EmbeddableCloudServer server;

    public static void main(String[] args) throws Exception {
        new Main().execute(args);
    }

    /** {@inheritDoc} */
    @Override
    protected void run(Injector injector) throws Exception {
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                kill();
            }
        });
        EmbeddableCloudServer server = new EmbeddableCloudServer(port);
        server.start();
        this.server = server; // only set it if it started
        System.out.println("Wuhuu Maritime Cloud Server started! Running on port " + port);
        System.out.println("Use CTRL+C to stop it");
    }

    void kill() {
        EmbeddableCloudServer server = this.server;
        if (server != null) {
            server.shutdown();
            try {
                for (int i = 0; i < 30; i++) {
                    if (!server.awaitTerminated(1, TimeUnit.SECONDS)) {
                        System.out.println("Awaiting shutdown " + i + " / 30 seconds");
                    }
                }
                throw new IllegalStateException("Could not shutdown server properly");
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
