package com.gradle.export.client.event.processor;

import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.gradle.export.client.event.BuildEvent;
import com.gradle.export.client.event.ExportBuild;
import com.gradle.scan.eventmodel.BuildSrcBuildFinished_1_0;
import com.gradle.scan.eventmodel.BuildSrcBuildStarted_1_0;
import com.gradle.scan.eventmodel.EventData;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ratpack.exec.Promise;
import ratpack.exec.util.SerialBatch;
import ratpack.func.Factory;
import ratpack.registry.MutableRegistry;
import ratpack.registry.Registry;
import ratpack.util.Types;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class EventStreamProcessing {

    private static final Logger LOGGER = LoggerFactory.getLogger(EventStreamProcessing.class);
    private final List<EventStreamReduction<?>> reductions;
    private final Map<Class<? extends EventData>, List<EventHandler<?>>> handlersByType = new IdentityHashMap<>();
    private final Map<Class<? extends EventData>, List<EventHandler<?>>> handlersByAssignableType = new IdentityHashMap<>();

    EventStreamProcessing(
        Iterable<EventStreamReducer<?>> reducers,
        ExportBuild build
    ) throws Exception {
        ImmutableList.Builder<EventStreamReduction<?>> reductions = ImmutableList.builder();

        for (EventStreamReducer<?> reducer : reducers) {
            EventStreamReduction<?> reduction = toReduction(reducer, build);
            if (reducer.onlyIf.apply(build, reduction.handlers.keySet())) {
                reductions.add(reduction);
                reduction.handlers.entries().forEach(entry ->
                    handlersByType.computeIfAbsent(entry.getKey(), l -> new ArrayList<>()).add(entry.getValue())
                );
                reduction.handlersOfOptionallySupportedEvents.entries().forEach(entry ->
                    handlersByType.computeIfAbsent(entry.getKey(), l -> new ArrayList<>()).add(entry.getValue())
                );
            }
        }

        this.reductions = reductions.build();
    }

    private static <T> EventStreamReduction<T> toReduction(EventStreamReducer<T> reducer, ExportBuild build) throws Exception {
        EventStreamReductionBuilder<T> builder = new EventStreamReductionBuilder<>(reducer.type, reducer.dependencies, build);
        return reducer.template.build(builder);
    }

    Set<Class<? extends EventData>> getEventDataTypes() {
        return ImmutableSet.<Class<? extends EventData>>builder()
            .addAll(handlersByType.keySet())
            .add(BuildSrcBuildStarted_1_0.class)
            .add(BuildSrcBuildFinished_1_0.class)
            .build();
    }

    Promise<Registry> process(Publisher<BuildEvent<?>> stream) {
        assertOrdering();

        Promise<Registry> promise = Promise.async(down ->
            stream.subscribe(new Subscriber<BuildEvent<?>>() {

                private Subscription subscription;
                private Throwable error;

                @Override
                public void onSubscribe(Subscription s) {
                    this.subscription = s;
                    subscription.request(1);
                }

                @Override
                public void onNext(BuildEvent<?> buildEvent) {
                    if (error == null) {
                        try {
                            fireEvent(buildEvent);
                        } catch (Throwable e) {
                            subscription.cancel();
                            error = e;
                            down.error(e);
                        }
                        subscription.request(1);
                    }
                }

                @Override
                public void onError(Throwable t) {
                    if (error == null) {
                        down.error(t);
                    } else {
                        LOGGER.warn("Event loader exception ignored after error from event processor", t);
                    }
                }

                @Override
                public void onComplete() {
                    if (error == null) {
                        try {
                            finalizeRegistry().connect(down);
                        } catch (Exception e) {
                            down.error(e);
                        }
                    }
                }
            })
        );

        return promise;
    }

    private void assertOrdering() {
        // assert that dependencies are never fulfilled downstream (this supports conditionally present reducers)
        Set<Class<?>> dependencies = new HashSet<>();
        for (EventStreamReduction<?> reduction : reductions) {
            assert !dependencies.contains(reduction.type) : String.format("Reducer creates object of type %s that an upstream reducer depends on", reduction.type);
            dependencies.addAll(reduction.dependencies);
        }
    }

    private void fireEvent(BuildEvent<?> event) {
        fireHandlers(event.getType().getClazz(), event);
    }

    private <T extends EventData> void fireHandlers(Class<T> type, BuildEvent<?> event) {
        BuildEvent<T> castEvent = event.cast(type);
        List<EventHandler<T>> handlers = handlersFor(type);
        handlers.forEach(h -> {
            try {
                h.handle(castEvent);
            } catch (RuntimeException e) {
                throw new IllegalStateException("Error in handler for event: " + castEvent, e);
            }
        });
    }

    private <T extends EventData> List<EventHandler<T>> handlersFor(Class<T> type) {
        List<EventHandler<?>> handlers = handlersByAssignableType.get(type);
        if (handlers == null) {
            handlers = new ArrayList<>();
            for (Map.Entry<Class<? extends EventData>, List<EventHandler<?>>> handlerEntry : handlersByType.entrySet()) {
                if (handlerEntry.getKey().isAssignableFrom(type)) {
                    handlers.addAll(handlerEntry.getValue());
                }
            }
            handlersByAssignableType.put(type, handlers);
        }

        return Types.cast(handlers);
    }

    private Promise<Registry> finalizeRegistry() throws Exception {
        Factory<? extends Promise<Registry>> factory = () -> {
            MutableRegistry registry = Registry.mutable();
            return SerialBatch.of(Collections2.transform(reductions, r -> doAdd(registry, r)))
                .yield()
                .map(l -> registry);
        };
        return Promise.flatten(factory);
    }

    private <T> Promise<T> doAdd(MutableRegistry registry, EventStreamReduction<T> r) {
        return r.factory.apply(registry).next(v -> {
            if (v != null) {
                // null check can be removed once https://github.com/ratpack/ratpack/issues/1144 is fixed
                registry.add(r.type, v);
            }
        });
    }
}
