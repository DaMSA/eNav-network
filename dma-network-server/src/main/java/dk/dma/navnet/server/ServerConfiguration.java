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

import com.beust.jcommander.Parameter;

import dk.dma.enav.model.shore.ServerId;

/**
 * 
 * @author Kasper Nielsen
 */
public class ServerConfiguration {

    /** The default port this server is running on. */
    public static final int DEFAULT_PORT = 43234;

    /** The default port the web server is running on. */
    public static final int DEFAULT_WEBSERVER_PORT = 8080;

    /** The id of the server, hard coded for now */
    ServerId id = new ServerId(1);

    @Parameter(names = "-port", description = "The port to listen on")
    int port = ServerConfiguration.DEFAULT_PORT;

    @Parameter(names = "-port", description = "The webserver port for the administrative interface")
    int webserverport = ServerConfiguration.DEFAULT_WEBSERVER_PORT;

    /**
     * @return the id
     */
    public ServerId getId() {
        return id;
    }

    /**
     * @return the serverPort
     */
    public int getServerPort() {
        return port;
    }

    /**
     * @return the webserverPort
     */
    public int getWebserverPort() {
        return webserverport;
    }

    /**
     * @param id
     *            the id to set
     */
    public ServerConfiguration setId(ServerId id) {
        this.id = id;
        return this;
    }

    /**
     * @param serverPort
     *            the serverPort to set
     */
    public ServerConfiguration setServerPort(int port) {
        this.port = port;
        return this;
    }

    /**
     * @param webserverPort
     *            the webserverPort to set
     */
    public ServerConfiguration setWebserverPort(int webserverport) {
        this.webserverport = webserverport;
        return this;
    }

    public static ServerConfiguration from(int port) {
        ServerConfiguration conf = new ServerConfiguration();
        conf.setServerPort(port);
        return conf;
    }
}
