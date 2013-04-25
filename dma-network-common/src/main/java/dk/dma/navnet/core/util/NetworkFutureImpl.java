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
package dk.dma.navnet.core.util;

import static java.util.Objects.requireNonNull;

import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import jsr166e.CompletableFuture;
import dk.dma.enav.communication.ConnectionFuture;
import dk.dma.enav.util.function.BiConsumer;

/**
 * 
 * @author Kasper Nielsen
 */
public class NetworkFutureImpl<T> extends CompletableFuture<T> implements ConnectionFuture<T> {
    final ScheduledExecutorService ses;

    final String requestId;

    NetworkFutureImpl(ScheduledExecutorService ses) {
        this.ses = ses;
        this.requestId = "fixme";
    }

    public NetworkFutureImpl<T> timeout(final long timeout, final TimeUnit unit) {
        if (ses == null) {
            throw new UnsupportedOperationException("timeout not supported.");
        }
        final NetworkFutureImpl<T> cf = new NetworkFutureImpl<>(ses);
        final Future<?> f = ses.schedule(new Runnable() {
            public void run() {
                if (!isDone()) {
                    cf.completeExceptionally(new TimeoutException("Timed out after " + timeout + " "
                            + unit.toString().toLowerCase()));
                }
            }
        }, timeout, unit);
        // Check if scheduler is shutdown
        // do it after cf.f is set (reversed in shutdown code)
        if (ses.isShutdown()) {
            f.cancel(false);
        }
        // The peek method could also just take a Runnable. But I see no reason not to take the 2 parameters.
        handle(new BiFun<T, Throwable, Void>() {
            public Void apply(T t, Throwable throwable) {
                // Users must manually purge if many outstanding tasks
                f.cancel(false);
                if (throwable != null) {
                    cf.completeExceptionally(throwable);
                } else {
                    cf.complete(t);
                }
                return null;
            }
        });
        return cf;
    }

    /** {@inheritDoc} */
    @Override
    public void handle(final BiConsumer<T, Throwable> consumer) {
        requireNonNull(consumer, "consumer is null");
        handle(new BiFun<T, Throwable, Void>() {
            public Void apply(T a, Throwable b) {
                consumer.accept(a, b);
                return null;
            }
        });
    }

    // /** {@inheritDoc} */
    // @Override
    // public String getRequestId() {
    // return requestId;
    // }
}
