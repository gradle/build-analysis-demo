package com.gradle.export.client.event.processor;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.google.common.reflect.TypeToken;
import com.gradle.export.client.event.ExportBuild;
import com.gradle.export.client.event.PairedEvents;
import com.gradle.scan.eventmodel.EventData;
import ratpack.exec.Promise;
import ratpack.registry.Registry;

import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

import static com.google.common.collect.ImmutableMultimap.copyOf;

/**
 * Builds a strategy for transforming a events of a set of types into a single object.
 * <p>
 * This type is explicitly designed to work with the {@link EventStreamReductionTemplate} functional type.
 * This function takes an instance of this type, and is expected to invoke {@link #then(Function)} to return a {@link EventStreamReducer}.
 * This function typically has local state that is populated/mutated by event handling functions registered with {@link #on(Class, EventHandler)}.
 * Such functions close over this state.
 * Similarly, the function given to {@link #then(Function)} method typically reads this local state and creates a reduced object from it.
 */
public final class EventStreamReductionBuilder<T> {

    private final TypeToken<T> type;
    private final Set<Class<?>> dependencies;
    private final Multimap<Class<? extends EventData>, EventHandler<?>> handlers = ArrayListMultimap.create();
    private final Multimap<Class<? extends EventData>, EventHandler<?>> handlersOfOptionallySupportedEvents = ArrayListMultimap.create();

    // Templates can access this for build metadata
    public final ExportBuild build;

    EventStreamReductionBuilder(TypeToken<T> type, Set<Class<?>> dependencies, ExportBuild build) {
        this.type = type;
        this.dependencies = ImmutableSet.copyOf(dependencies);
        this.build = build;
    }

    /*
        Ideas:
        - Add some sugar methods here to fail fast and be more communicative about how to process.
            e.g. onOnly(), requireOne() etc.
     */

    /**
     * Register a handler for all events that are assignment compatible with the given type.
     * The given event type is expected to be supported by the source build.
     */
    public <E extends EventData> EventStreamReductionBuilder<T> on(Class<E> eventType, EventHandler<? super E> action) {
        handlers.put(eventType, action);
        return this;
    }

    /**
     * Register a handler for all events that are assignment compatible with the given type.
     * The given event type might not be supported by the source build.
     */
    public <E extends EventData> EventStreamReductionBuilder<T> onOptionallySupported(Class<E> eventType, EventHandler<? super E> action) {
        handlersOfOptionallySupportedEvents.put(eventType, action);
        return this;
    }

    public <S extends EventData, F extends EventData, K> EventStreamReductionBuilder<T> pairing(
        Class<S> startEventType, Function<? super S, ? extends K> startKeyExtractor,
        Class<F> finishEventType, Function<? super F, ? extends K> finishKeyExtractor,
        Consumer<? super PairedEvents<S, F>> consumer
    ) {
        EventPairer<S, F, K> pairer = EventPairer.of(startEventType, startKeyExtractor, finishEventType, finishKeyExtractor, consumer);
        return on(pairer.startType, pairer::start).on(pairer.finishType, pairer::finish);
    }

    public EventStreamReductionBuilder<T> attach(EventStreamListener eventStreamListener) {
        eventStreamListener.addListeners(this);
        return this;
    }

    /**
     * Registers the factory that will be invoked to create the reduction.
     * <p>
     * The factory will be invoked after all events of the registered types have been processed.
     * <p>
     * This method returns a built reducer, and is typically the last thing called in a {@link EventStreamReductionTemplate} function.
     */
    public EventStreamReduction<T> then(Function<? super Registry, ? extends T> factory) {
        return thenAsync(r -> Promise.sync(() -> factory.apply(r)));
    }

    public EventStreamReduction<T> thenAsync(Function<? super Registry, ? extends Promise<T>> factory) {
        return new EventStreamReduction<>(type, dependencies, copyOf(handlers), copyOf(handlersOfOptionallySupportedEvents), factory);
    }

}
