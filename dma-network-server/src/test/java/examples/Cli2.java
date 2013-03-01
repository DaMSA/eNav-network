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

import dk.dma.enav.net.MaritimeNetworkConnection;
import dk.dma.enav.net.broadcast.BroadcastProperties;
import dk.dma.enav.util.function.BiConsumer;
import dk.dma.navnet.client.MaritimeNetworkConnectionBuilder;

/**
 * 
 * @author Kasper Nielsen
 */
public class Cli2 {
    public static void main(String[] args) throws Exception {
        MaritimeNetworkConnectionBuilder b = MaritimeNetworkConnectionBuilder.create("mmsi://1234");
        try (MaritimeNetworkConnection c = b.connect()) {
            c.subscribe(HejMedDig.class, new BiConsumer<BroadcastProperties, HejMedDig>() {
                public void accept(BroadcastProperties l, HejMedDig r) {
                    System.out.println("fik beskeden " + r.getMessage() + " fra " + l.getId());
                }
            });

            Thread.sleep(30000);
        }
    }
}
