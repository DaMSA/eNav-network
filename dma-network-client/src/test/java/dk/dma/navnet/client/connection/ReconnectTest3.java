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
package dk.dma.navnet.client.connection;

import org.junit.Test;

import dk.dma.enav.maritimecloud.MaritimeCloudClient;
import dk.dma.navnet.client.AbstractClientConnectionTest;
import dk.dma.navnet.messages.auxiliary.ConnectedMessage;
import dk.dma.navnet.messages.auxiliary.HelloMessage;

/**
 * 
 * @author Kasper Nielsen
 */
public class ReconnectTest3 extends AbstractClientConnectionTest {

    @Test
    public void disConnectWhileConnecting() throws Exception {
        @SuppressWarnings("unused")
        MaritimeCloudClient c = create();
        t.take(HelloMessage.class);
        t.close();
        // we will try to reconnect anyways
        t.take(HelloMessage.class);
        t.send(new ConnectedMessage("ABC", 0));

    }
}
