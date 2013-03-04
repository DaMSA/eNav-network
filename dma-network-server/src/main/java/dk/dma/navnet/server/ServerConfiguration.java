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

/**
 * 
 * @author Kasper Nielsen
 */
public class ServerConfiguration {
    private boolean isSecure;
    private int port;

    /**
     * @return the port
     */
    public int getPort() {
        return port;
    }

    /**
     * @return the isSecure
     */
    public boolean isSecure() {
        return isSecure;
    }

    /**
     * @param port
     *            the port to set
     */
    public ServerConfiguration setPort(int port) {
        this.port = port;
        return this;
    }

    /**
     * @param isSecure
     *            the isSecure to set
     */
    public ServerConfiguration setSecure(boolean isSecure) {
        this.isSecure = isSecure;
        return this;
    }

    public ENavNetworkServer create() {
        return null;
    }
}
