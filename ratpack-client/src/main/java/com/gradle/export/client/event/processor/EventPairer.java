package com.gradle.export.client.event.processor;

import com.gradle.export.client.event.BuildEvent;
import com.gradle.export.client.event.PairedEvents;
import com.gradle.scan.eventmodel.EventData;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

final class EventPairer<S extends EventData, F extends EventData, K> implements Consumer<EventStreamReductionBuilder<?>> {

    private final Map<K, BuildEvent<S>> started = new HashMap<>();

    final Class<S> startType;
    private final Function<? super S, ? extends K> startKeyExtractor;

    final Class<F> finishType;
    private final Function<? super F, ? extends K> finishKeyExtractor;

    private final Consumer<? super PairedEvents<S, F>> consumer;

    static <S extends EventData, F extends EventData, K> EventPairer<S, F, K> of(
        Class<S> startType, Function<? super S, ? extends K> startKeyExtractor,
        Class<F> finishType, Function<? super F, ? extends K> finishKeyExtractor,
        Consumer<? super PairedEvents<S, F>> consumer
    ) {
        return new EventPairer<>(startType, startKeyExtractor, finishType, finishKeyExtractor, consumer);
    }

    private EventPairer(Class<S> startType, Function<? super S, ? extends K> startKeyExtractor, Class<F> finishType, Function<? super F, ? extends K> finishKeyExtractor, Consumer<? super PairedEvents<S, F>> consumer) {
        this.startType = startType;
        this.startKeyExtractor = startKeyExtractor;
        this.finishType = finishType;
        this.finishKeyExtractor = finishKeyExtractor;
        this.consumer = consumer;
    }

    public void start(BuildEvent<S> startEvent) {
        K key = startKeyExtractor.apply(startEvent.getData());
        if (key == null) {
            throw new IllegalStateException("Key extractor returned null key for: " + startEvent);
        }

        BuildEvent<S> existing = started.put(key, startEvent);
        if (existing != null) {
            throw new IllegalStateException("Overlapping start events with same key: " + key + " from start event " + startEvent + ", " + existing);
        }
    }

    public void finish(BuildEvent<F> finishEvent) {
        K key = finishKeyExtractor.apply(finishEvent.getData());
        BuildEvent<S> startEvent = started.get(key);
        if (startEvent == null) {
            throw new IllegalStateException("Could not find start event for key " + key + " from finish event " + finishEvent + "(entries: " + started + ")");
        }

        consumer.accept(new PairedEvents<>(startEvent, finishEvent));
    }

    @Override
    public void accept(EventStreamReductionBuilder<?> builder) {
        builder.on(startType, this::start).on(finishType, this::finish);
    }

}
