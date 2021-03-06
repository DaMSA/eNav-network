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
package dk.dma.navnet.server.requests;

import dk.dma.navnet.messages.s2c.ServerRequestMessage;
import dk.dma.navnet.messages.s2c.ServerResponseMessage;
import dk.dma.navnet.server.connection.ServerConnection;

/**
 * 
 * @author Kasper Nielsen
 */
public interface RequestProcessor<S extends ServerRequestMessage<T>, T extends ServerResponseMessage> {
    T process(ServerConnection connection, S message) throws RequestException;
}
