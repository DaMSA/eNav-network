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
package dk.dma.navnet.core.transport;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * 
 * @author Kasper Nielsen
 */
public abstract class ClientTransportFactory {

    /**
     * Connects using the specified transport.
     * 
     * @param transport
     *            the transport to use
     * @param timeout
     *            the connection time out
     * @param unit
     *            the unit of timeout
     * @throws IOException
     *             could not connect
     */
    public abstract void connect(Transport transport, long timeout, TimeUnit unit) throws IOException;

    /**
     * Shuts down the factory.
     * 
     * @throws IOException
     */
    public abstract void shutdown() throws IOException;

}
