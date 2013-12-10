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
package dk.dma.navnet.client.connection;

import static java.util.Objects.requireNonNull;
import dk.dma.enav.maritimecloud.ClosingCode;

/**
 * 
 * @author Kasper Nielsen
 */
class ClientDisconnectFuture implements Runnable {

    final ClientConnection connection;

    private final ClientTransport transport;

    ClientDisconnectFuture(ClientConnection connection, ClientTransport transport) {
        this.connection = requireNonNull(connection);
        this.transport = transport;
    }

    /** {@inheritDoc} */
    @Override
    public void run() {
        // TODO send poison pill
        transport.doClose(ClosingCode.NORMAL);
        // ClientTransport transport = new ClientTransport(connection, reconnectId);
        // ConnectionManager cm = connection.connectionManager;
        // cm.getWebsocketContainer().connectToServer(transport, cm.uri);
    }

    // void connect2() {
    //
    // ConnectionState state = this.state;
    // if (state == ConnectionState.NOT_CONNECTED) {
    // this.transport = new ClientTransport(this, -1);
    // this.state = ConnectionState.CONNECTING;
    // connectingFuture = connectionManager.threadManager.submit(new Callable<Void>() {
    // public Void call() throws Exception {
    // connect2();
    // return null;
    // }
    // });
    //
    // if (state != ConnectionState.CONNECTING) {
    // return;
    // }
    // LOG.info("Connecting to " + connectionManager.uri);
    // try {
    // connectionManager.getWebsocketContainer().connectToServer(transport, connectionManager.uri);
    // } catch (IOException e) {
    // e.printStackTrace();
    // } catch (DeploymentException e1) {
    // e1.printStackTrace();
    // }
    // }

}
