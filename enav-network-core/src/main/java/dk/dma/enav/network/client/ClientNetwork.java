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
package dk.dma.enav.network.client;

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.google.common.net.HostAndPort;

import dk.dma.enav.model.MaritimeId;
import dk.dma.enav.model.geometry.Area;
import dk.dma.enav.model.geometry.PositionTime;
import dk.dma.enav.net.MaritimeNetworkConnection;
import dk.dma.enav.net.ServiceCallback;
import dk.dma.enav.net.ServiceRegistration;
import dk.dma.enav.network.MaritimeNetworkConnectionBuilder;
import dk.dma.enav.network.Packets;
import dk.dma.enav.network.PersistentConnection;
import dk.dma.enav.service.spi.InitiatingMessage;
import dk.dma.enav.service.spi.MaritimeBroadcastMessage;
import dk.dma.enav.service.spi.MaritimeService;
import dk.dma.enav.service.spi.MaritimeServiceMessage;
import dk.dma.enav.util.function.Supplier;

/**
 * 
 * @author Kasper Nielsen
 */
public class ClientNetwork implements MaritimeNetworkConnection {

    PersistentConnection connection;

    private final ConcurrentHashMap<Class<? extends MaritimeService>, ServiceCallback<?, ?>> registeredServices = new ConcurrentHashMap<>();

    private volatile State state;

    private final CountDownLatch terminated = new CountDownLatch(1);

    final ScheduledExecutorService ses = Executors.newSingleThreadScheduledExecutor();

    final ExecutorService es = Executors.newCachedThreadPool();

    /**
     * @param id
     */
    protected ClientNetwork(MaritimeId id) {

    }

    public static MaritimeNetworkConnection connect(MaritimeNetworkConnectionBuilder builder) throws IOException {
        final ClientNetwork cc = new ClientNetwork(builder.getId());
        HostAndPort hap = HostAndPort.fromString(builder.getNodes());
        final PersistentConnection mc = Client2ServerConnection.connect(cc, builder.getId(),
                new InetSocketAddress(hap.getHostText(), hap.getPort()));

        cc.connection = mc;
        final Supplier<PositionTime> positionSupplier = builder.getPositionSupplier();
        if (positionSupplier != null) {
            cc.ses.scheduleAtFixedRate(new Runnable() {
                @Override
                public void run() {
                    PositionTime pt = positionSupplier.get();
                    if (pt != null) {
                        try {
                            mc.packetWrite(Packets.positionReport(pt));
                        } catch (Throwable t) {
                            t.printStackTrace();
                        }
                    }
                }
            }, 0, 1, TimeUnit.SECONDS);
        }
        cc.manage();
        return cc;
    }

    /** {@inheritDoc} */
    @Override
    public boolean awaitFullyClosed(long timeout, TimeUnit unit) throws InterruptedException {
        return terminated.await(timeout, unit);
    }

    /** {@inheritDoc} */
    @Override
    public synchronized void close() {
        if (isClosed()) {
            return;
        }
        state = State.SHUTDOWN;
        connection.close();
    }

    /** {@inheritDoc} */
    @Override
    public NetworkFutureImpl<Map<MaritimeId, PositionTime>> findAll(Area shape) {
        return connection.withReply(null, Packets.FIND_SERVICE, shape);
    }

    /** {@inheritDoc} */
    @Override
    public NetworkFutureImpl<Map<MaritimeId, Class<? extends MaritimeService>>> findServices(
            Class<? extends MaritimeService> serviceType) {
        return connection.withReply(null, Packets.FIND_SERVICE, serviceType);
    }

    /** {@inheritDoc} */
    @Override
    public <T, S extends MaritimeServiceMessage<T> & InitiatingMessage> NetworkFutureImpl<T> invokeService(
            MaritimeId id, S msg) {
        return connection.withReply(id, Packets.INVOKE_SERVICE, msg);
    }

    /** {@inheritDoc} */
    @Override
    public boolean isClosed() {
        return state == State.SHUTDOWN || state == State.CLOSED;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isFullyClosed() {
        return state == State.CLOSED;
    }

    void manage() {
        // ManagementFactory.getPlatformMBeanServer().registerMBean(Managements.tryCreate(connection, "connection"),
        // new ObjectName("dk.dma.net:name=connection"));
    }

    /**
     * {@inheritDoc}
     */
    void packetRead0(final PersistentConnection con, final Packets p) throws Exception {
        if (p.b == Packets.CONNECTION_DISCONNECTED) {
            synchronized (this) {
                state = State.CLOSED;
                ses.shutdown();
                es.shutdown();
                terminated.countDown();
                registeredServices.clear();
            }
        } else if (p.b == Packets.INVOKE_SERVICE) {
            MaritimeServiceMessage<?> m = p.payloadToObject();
            Class<? extends MaritimeService> serviceType = m.getServiceType();
            @SuppressWarnings("unchecked")
            ServiceCallback<Object, Object> impl = (ServiceCallback<Object, Object>) registeredServices
                    .get(serviceType);
            impl.process(m, new ServiceCallback.Context<Object>() {
                public void complete(Object result) {
                    requireNonNull(result);
                    con.packetWrite(p.replyWith(result));
                }

                public void fail(Throwable cause) {
                    requireNonNull(cause);
                    con.packetWrite(p.replyWithFailure(cause));
                }
            });
        } else {
            System.out.println("Dont know about " + p.b);
        }
    }

    /** {@inheritDoc} */
    @Override
    public <T extends MaritimeServiceMessage<?>, S extends MaritimeService, E extends MaritimeServiceMessage<T> & InitiatingMessage> ServiceRegistration registerService(
            S service, ServiceCallback<E, T> b) {
        if (registeredServices.putIfAbsent(service.getClass(), b) != null) {
            throw new IllegalArgumentException("A service of the specified type has already been registered");
        }
        final NetworkFutureImpl<Void> stp = connection.withReply(null, Packets.REGISTER_SERVICE, service);
        return new ServiceRegistration() {

            @Override
            public boolean awaitRegistered(long timeout, TimeUnit unit) {
                try {
                    stp.get(timeout, unit);
                } catch (InterruptedException | ExecutionException | TimeoutException e) {
                    throw new RuntimeException(e);
                }
                return true;
            }

            @Override
            public void cancel() {
                throw new UnsupportedOperationException();
            }
        };
    }

    /** {@inheritDoc} */
    @Override
    public NetworkFutureImpl<Void> broadcast(MaritimeBroadcastMessage message) {
        // connection.sendTo(id, new MessageTransportNetworkMessage(message));
        throw new UnsupportedOperationException();
    }

    enum State {
        CLOSED, CONNECTED, SHUTDOWN;
    }
}
