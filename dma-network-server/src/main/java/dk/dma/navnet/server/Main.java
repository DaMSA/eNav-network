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
package dk.dma.navnet.server;

import java.util.concurrent.TimeUnit;

import com.beust.jcommander.Parameter;
import com.google.inject.Injector;

import dk.dma.commons.app.AbstractCommandLineTool;

/**
 * 
 * @author Kasper Nielsen
 */
public class Main extends AbstractCommandLineTool {

    @Parameter(names = "-port", description = "The port to listen on")
    int port = ENavNetworkServer.DEFAULT_PORT;
    volatile ENavNetworkServer server;

    public static void main(String[] args) throws Exception {
        // args = new String[] { "-source", "ais163.sealan.dk:65262", "-store", "localhost" };
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
        ENavNetworkServer server = new ENavNetworkServer(port);
        server.start();
        this.server = server; // only set it if it started
        System.out.println("server running on port " + port);
    }

    void kill() {
        ENavNetworkServer server = this.server;
        if (server != null) {
            server.shutdown();
            try {
                if (!server.awaitTerminated(30, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("Could not shutdown server properly");
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

}
