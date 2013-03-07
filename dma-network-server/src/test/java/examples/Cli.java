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
package examples;

import java.util.Date;

import dk.dma.enav.communication.MaritimeNetworkConnection;
import dk.dma.navnet.client.MaritimeNetworkConnectionBuilder;

/**
 * 
 * @author Kasper Nielsen
 */
public class Cli {

    public static void main(String[] args) throws Exception {
        MaritimeNetworkConnectionBuilder b = MaritimeNetworkConnectionBuilder.create("mmsi://12355555");

        HejMedDig dig = new HejMedDig();
        try (MaritimeNetworkConnection c = b.connect()) {
            for (;;) {
                dig.setMessage("LadOsPise " + new Date());
                c.broadcast(dig);
                Thread.sleep(5000);
            }
        }
    }
}
