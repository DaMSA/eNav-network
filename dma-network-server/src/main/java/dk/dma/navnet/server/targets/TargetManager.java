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
package dk.dma.navnet.server.targets;

import static java.util.Objects.requireNonNull;

import java.util.Collections;
import java.util.Iterator;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import jsr166e.ConcurrentHashMapV8;
import jsr166e.ConcurrentHashMapV8.Action;
import jsr166e.ConcurrentHashMapV8.Fun;

import org.picocontainer.Startable;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import dk.dma.commons.tracker.PositionTracker;
import dk.dma.enav.model.MaritimeId;
import dk.dma.enav.model.geometry.Area;
import dk.dma.enav.model.geometry.PositionTime;
import dk.dma.enav.util.function.BiConsumer;
import dk.dma.enav.util.function.Consumer;
import dk.dma.navnet.server.connection.ServerConnection;

/**
 * 
 * @author Kasper Nielsen
 */
public class TargetManager implements Startable, Iterable<Target> {

    private final ScheduledExecutorService ses = Executors.newSingleThreadScheduledExecutor(new ThreadFactoryBuilder()
            .setNameFormat("PositionTrackerUpdate").setDaemon(true).build());

    private final ConcurrentHashMapV8<String, Target> targets = new ConcurrentHashMapV8<>();

    final PositionTracker<Target> tracker = new PositionTracker<>();

    public Target find(MaritimeId id) {
        return targets.get(id.toString());
    }

    public void reportPosition(Target target, PositionTime pt) {
        tracker.update(target, pt);
    }

    public void forEachTarget(final Consumer<Target> consumer) {
        requireNonNull(consumer);
        targets.forEachValue(10, new Action<Target>() {
            public void apply(Target target) {
                consumer.accept(target);
            }
        });
    }

    public void forEachConnection(final Consumer<ServerConnection> consumer) {
        requireNonNull(consumer);
        targets.forEachValue(10, new Action<Target>() {
            public void apply(Target target) {
                ServerConnection c = target.getConnection();
                if (c != null) {
                    consumer.accept(c);
                }
            }
        });
    }

    /**
     * @param shape
     * @param block
     * @see dk.dma.commons.tracker.PositionTracker#forEachWithinArea(dk.dma.enav.model.geometry.Area,
     *      dk.dma.enav.util.function.BiConsumer)
     */
    public void forEachWithinArea(Area shape, BiConsumer<Target, PositionTime> block) {
        tracker.forEachWithinArea(shape, block);
    }

    public Target getTarget(final MaritimeId id) {
        Target target = targets.computeIfAbsent(id.toString(), new Fun<String, Target>() {
            public Target apply(String key) {
                return new Target(TargetManager.this, id);
            }
        });
        return target;
    }

    /** {@inheritDoc} */
    @Override
    public Iterator<Target> iterator() {
        return Collections.unmodifiableCollection(targets.values()).iterator();
    }

    /** {@inheritDoc} */
    @Override
    public void start() {
        tracker.schedule(ses, 1000);
    }

    /** {@inheritDoc} */
    @Override
    public void stop() {
        ses.shutdown();
    }
}
