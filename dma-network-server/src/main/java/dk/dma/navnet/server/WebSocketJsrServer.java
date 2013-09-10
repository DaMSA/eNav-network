package dk.dma.navnet.server;

import javax.websocket.OnMessage;
import javax.websocket.Session;
import javax.websocket.server.ServerContainer;
import javax.websocket.server.ServerEndpoint;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.websocket.jsr356.server.WebSocketConfiguration;

/**
 * Example of setting up a javax.websocket server with Jetty embedded
 */
public class WebSocketJsrServer {
    /**
     * A server socket endpoint
     */
    @ServerEndpoint(value = "/echo")
    public static class EchoJsrSocket {
        @OnMessage
        public void onMessage(Session session, String message) {
            session.getAsyncRemote().sendText(message);
        }
    }

    public static void main(String[] args) throws Exception {
        Server server = new Server(8080);

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
        wsContainer.addEndpoint(EchoJsrSocket.class);

        server.start();
        context.dumpStdErr();
        server.join();
    }

    // static class DumpServlet extends Servlet {

    // }
}
