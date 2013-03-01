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
package dk.dma.navnet.core.spi;

import org.eclipse.jetty.websocket.api.StatusCode;

/**
 * 
 * @author Kasper Nielsen
 */
public class CloseCodes {

    /**
     * The connection is being terminated because the endpoint received data of a type it cannot accept (for example, a
     * text-only endpoint received binary data).
     */
    public static final int BAD_DATA = StatusCode.BAD_DATA;
}
