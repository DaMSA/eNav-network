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

import static java.util.Objects.requireNonNull;
import dk.dma.enav.model.geometry.PositionTime;
import dk.dma.navnet.protocol.Application;

/**
 * There exist at most one target per remote client. A target does not necessarily have an active connection. But at
 * some point, some client with the specified id have connected.
 * 
 * @author Kasper Nielsen
 */
@SuppressWarnings("serial")
public class Target extends Application {

    /** The remote id. */
    private final String id;

    /** The latest reported time and position. */
    volatile PositionTime latestPosition;

    /**
     * @param key
     */
    public Target(String id) {
        this.id = requireNonNull(id);
    }

    public String getId() {
        return id;
    }
}
