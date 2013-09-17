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
package dk.dma.navnet.protocol.transport;

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.net.InetSocketAddress;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.websocket.DeploymentException;
import javax.websocket.server.ServerContainer;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.websocket.jsr356.server.WebSocketConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A factory used to create transports from connections by remote clients.
 * 
 * @author Kasper Nielsen
 */
public final class TransportServerFactory {

    /** The logger. */
    static final Logger LOG = LoggerFactory.getLogger(TransportServerFactory.class);

    /** The actual WebSocket server */
    private final Server server;

    private TransportServerFactory(InetSocketAddress sa) {
        this.server = new Server(sa);
        // Sets the sockets reuse address to true
        ServerConnector connector = (ServerConnector) server.getConnectors()[0];
        connector.setReuseAddress(true);
    }

    /**
     * Invoked whenever a client has connected.
     * 
     * @param supplier
     *            a supplier used for creating new transports
     * @throws IOException
     */
    public void startAccept(Class<? extends Transport> supplier) throws IOException {
        requireNonNull(supplier);

        // New handler
        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");
        server.setHandler(context);

        // Add a servlet to your context.
        // It is required that you provide at least 1 servlet.
        // Recommended that this servlet mere provide a
        // "This is a websocket only server" style response
        context.addServlet(new ServletHolder(new DumpServlet()), "/*");

        // Enable javax.websocket configuration for the context
        ServerContainer wsContainer = WebSocketConfiguration.configureContext(context);

        // Add your websockets to the container
        try {
            wsContainer.addEndpoint(supplier);
        } catch (DeploymentException e2) {
            e2.printStackTrace();
        }

        try {
            server.start();
            LOG.info("System is ready accept client connections");
        } catch (Exception e) {
            try {
                server.stop();
            } catch (Exception e1) {
                // We want to rethrow the original exception, so just log this one
                LOG.info("System failed to stop", e);
            }
            if (e.getCause() instanceof IOException) {
                throw (IOException) e.getCause();
            }
            throw new IOException(e);
        }
    }

    /**
     * Stops accepting any more connections
     * 
     * @throws IOException
     */
    public void shutdown() throws IOException {
        try {
            server.stop();
        } catch (Exception e) {
            if (e.getCause() instanceof IOException) {
                throw (IOException) e.getCause();
            }
            throw new IOException(e);
        }
    }

    /**
     * Creates a new transport server factory.
     * 
     * @param port
     *            the port to listen on
     * @return a new transport server factory
     */
    public static TransportServerFactory createServer(int port) {
        return createServer(new InetSocketAddress(port));
    }

    /**
     * Creates a new transport server factory.
     * 
     * @param port
     *            the address to bind to
     * @return a new transport server factory
     */
    public static TransportServerFactory createServer(InetSocketAddress address) {
        return new TransportServerFactory(address);
    }

    @SuppressWarnings("serial")
    static class DumpServlet extends HttpServlet {

        @Override
        protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException,
                IOException {
            response.setContentType("text/html");
            response.setStatus(HttpServletResponse.SC_OK);
            response.getWriter().println("<h1>DumpServlet</h1><pre>");
            response.getWriter().println("requestURI=" + request.getRequestURI());
            response.getWriter().println("contextPath=" + request.getContextPath());
            response.getWriter().println("servletPath=" + request.getServletPath());
            response.getWriter().println("pathInfo=" + request.getPathInfo());
            response.getWriter().println("session=" + request.getSession(true).getId());

            String r = request.getParameter("resource");
            if (r != null) {
                response.getWriter().println("resource(" + r + ")=" + getServletContext().getResource(r));
            }

            response.getWriter().println("</pre>");
        }
    }
}
