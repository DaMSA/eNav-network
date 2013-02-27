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
package dk.dma.navnet.client;

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.ReentrantLock;

import jsr166e.CompletableFuture;
import jsr166e.ConcurrentHashMapV8;
import dk.dma.enav.model.MaritimeId;
import dk.dma.enav.model.geometry.Area;
import dk.dma.enav.model.geometry.PositionTime;
import dk.dma.enav.net.MaritimeNetworkConnection;
import dk.dma.enav.net.NetworkFuture;
import dk.dma.enav.net.ServiceCallback;
import dk.dma.enav.net.ServiceRegistration;
import dk.dma.enav.net.broadcast.BroadcastMessage;
import dk.dma.enav.service.spi.InitiatingMessage;
import dk.dma.enav.service.spi.MaritimeService;
import dk.dma.enav.service.spi.MaritimeServiceMessage;
import dk.dma.enav.util.function.Consumer;
import dk.dma.enav.util.function.Supplier;
import dk.dma.navnet.core.messages.c2c.Broadcast;
import dk.dma.navnet.core.messages.c2c.InvokeService;
import dk.dma.navnet.core.messages.s2c.FindServices;
import dk.dma.navnet.core.messages.s2c.PositionReportMessage;
import dk.dma.navnet.core.messages.s2c.RegisterService;
import dk.dma.navnet.core.util.NetworkFutureImpl;

/**
 * 
 * @author Kasper Nielsen
 */
public class ClientNetwork implements MaritimeNetworkConnection {

    final MaritimeId clientId;

    ClientConnection connection;

    final ExecutorService es = Executors.newCachedThreadPool();

    private final ReentrantLock lock = new ReentrantLock();

    final ConcurrentHashMap<String, InternalServiceCallbackRegistration> registeredServices = new ConcurrentHashMap<>();

    private final ScheduledExecutorService ses = Executors.newSingleThreadScheduledExecutor();

    private volatile NetState state = NetState.CREATED;

    final ConcurrentHashMapV8<Class<? extends BroadcastMessage>, CopyOnWriteArraySet<Consumer<BroadcastMessage>>> subscribers = new ConcurrentHashMapV8<>();

    private final CountDownLatch terminated = new CountDownLatch(1);

    /**
     * @param clientId
     */
    protected ClientNetwork(MaritimeId clientId) {
        this.clientId = requireNonNull(clientId);
    }

    /** {@inheritDoc} */
    @Override
    public boolean awaitTerminated(long timeout, TimeUnit unit) throws InterruptedException {
        return terminated.await(timeout, unit);
    }

    /** {@inheritDoc} */
    @Override
    public NetworkFuture<Void> broadcast(BroadcastMessage message) {
        Broadcast b = new Broadcast(message.channel(), JSonUtil.persistAndEscape(message));
        connection.sendMessage(b);
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public void close() {
        lock.lock();
        try {
            if (isClosed()) {
                return;
            }
            state = NetState.CLOSED;
            ses.shutdown();
            try {
                ses.awaitTermination(1, TimeUnit.SECONDS);
            } catch (InterruptedException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            }
            try {
                connection.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            // skal lige have fundet ud af med det shutdown
            terminated.countDown();
        } finally {
            lock.unlock();
        }
    }

    /** {@inheritDoc} */
    @Override
    public NetworkFutureImpl<Map<MaritimeId, PositionTime>> findAll(Area shape) {
        return null;// connection.withReply(null, Packet.FIND_SERVICE, shape);
    }

    /** {@inheritDoc} */
    @Override
    public NetworkFuture<Map<MaritimeId, String>> findServices(final String serviceType) {
        return NetworkFutureImpl.wrap(connection.sendMessage(new FindServices(serviceType)).thenApply(
                new CompletableFuture.Fun<String[], Map<MaritimeId, String>>() {
                    @Override
                    public Map<MaritimeId, String> apply(String[] s) {
                        HashMap<MaritimeId, String> m = new HashMap<>();
                        for (String str : s) {
                            m.put(MaritimeId.create(str), serviceType);
                        }
                        return m;
                    }
                }));
    }

    /** {@inheritDoc} */
    @Override
    public <T, S extends MaritimeServiceMessage<T> & InitiatingMessage> NetworkFutureImpl<T> invokeService(
            MaritimeId id, S msg) {
        InvokeService is = new InvokeService(1, UUID.randomUUID().toString(), msg.serviceName(), msg.messageName(),
                JSonUtil.persistAndEscape(msg));
        is.setDestination(id.toString());
        is.setSource(clientId.toString());
        return connection.sendMessage(is);
    }

    /** {@inheritDoc} */
    @Override
    public boolean isClosed() {
        return state == NetState.TERMINATED || state == NetState.CLOSED;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isTerminated() {
        return state == NetState.TERMINATED;
    }

    void manage() {
        // ManagementFactory.getPlatformMBeanServer().registerMBean(Managements.tryCreate(connection, "connection"),
        // new ObjectName("dk.dma.net:name=connection"));
    }

    /**
     * {@inheritDoc}
     */
    // void incomingPacket(final PersistentConnection con, final Packet p) throws Exception {
    // if (p.b == Packet.CONNECTION_DISCONNECTED) {
    // lock.lock();
    // try {
    // state = NetState.TERMINATED;
    // ses.shutdown();
    // es.shutdown();
    // terminated.countDown();
    // registeredServices.clear();
    // } finally {
    // lock.unlock();
    // }
    // } else if (p.b == Packet.INVOKE_SERVICE) {
    // MaritimeServiceMessage<?> m = p.payloadToObject();
    // Class<? extends MaritimeService> serviceType = m.getServiceType();
    // @SuppressWarnings("unchecked")
    // ServiceCallback<Object, Object> impl = (ServiceCallback<Object, Object>) registeredServices
    // .get(serviceType);
    // impl.process(m, new ServiceCallback.Context<Object>() {
    // public void complete(Object result) {
    // requireNonNull(result);
    // con.packetWrite(p.replyWith(result));
    // }
    //
    // public void fail(Throwable cause) {
    // requireNonNull(cause);
    // con.packetWrite(p.replyWithFailure(cause));
    // }
    // });
    // } else {
    // System.out.println("Dont know about " + p.b);
    // }
    // }

    /** {@inheritDoc} */
    @Override
    public <T extends MaritimeServiceMessage<?>, S extends MaritimeService, E extends MaritimeServiceMessage<T> & InitiatingMessage> ServiceRegistration registerService(
            S service, ServiceCallback<E, T> b) {
        if (registeredServices.putIfAbsent(service.getName(),
                new InternalServiceCallbackRegistration(service.getClass(), b)) != null) {
            throw new IllegalArgumentException("A service of the specified type has already been registered");
        }
        final NetworkFutureImpl<Void> stp = connection.sendMessage(new RegisterService(service.getName()));
        // final NetworkFutureImpl<Void> stp = null;// connection.withReply(null, Packet.REGISTER_SERVICE, service);
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
    @SuppressWarnings("unchecked")
    @Override
    public <T extends BroadcastMessage> void subscribe(Class<T> messageType, Consumer<T> consumer) {
        CopyOnWriteArraySet<Consumer<BroadcastMessage>> set = subscribers
                .computeIfAbsent(
                        messageType,
                        new ConcurrentHashMapV8.Fun<Class<? extends BroadcastMessage>, CopyOnWriteArraySet<Consumer<BroadcastMessage>>>() {
                            public CopyOnWriteArraySet<Consumer<BroadcastMessage>> apply(
                                    Class<? extends BroadcastMessage> t) {
                                return new CopyOnWriteArraySet<>();
                            }
                        });

        set.add((Consumer<BroadcastMessage>) consumer);
    }

    public static MaritimeNetworkConnection create(MaritimeNetworkConnectionBuilder builder) throws IOException {
        final ClientNetwork cc = new ClientNetwork(builder.getId());
        // HostAndPort hap = HostAndPort.fromString(builder.getNodes());

        // final PersistentConnection mc = Client2ServerConnection.connect(cc, builder.getId(),
        // new InetSocketAddress(hap.getHostText(), hap.getPort()));

        final ClientConnection mc = new ClientConnection("ws://localhost:11111", cc);
        try {
            mc.connect();
        } catch (Exception e) {
            throw new IOException(e);
        }

        cc.connection = mc;
        final Supplier<PositionTime> positionSupplier = builder.getPositionSupplier();
        if (positionSupplier != null) {
            cc.ses.scheduleAtFixedRate(new Runnable() {
                @Override
                public void run() {
                    try {
                        PositionTime pt = positionSupplier.get();
                        if (pt != null) {
                            mc.sendMessage(new PositionReportMessage(pt));
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }, 0, 1, TimeUnit.SECONDS);
        }
        cc.manage();
        return cc;
    }

    public static void main(String[] args) throws NoSuchFieldException, SecurityException {
        System.out.println(ClientNetwork.class.getField("NAME").getAnnotation(Deprecated.class));
    }

    enum NetState {
        CLOSED, CONNECTED, CREATED, TERMINATED;
    }
}
