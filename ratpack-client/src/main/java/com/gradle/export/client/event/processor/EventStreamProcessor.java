package com.gradle.export.client.event.processor;

import com.google.common.collect.ImmutableList;
import com.google.common.reflect.TypeToken;
import com.gradle.export.client.event.ExportBuild;
import com.gradle.export.client.event.VersionedEventType;
import ratpack.exec.Promise;
import ratpack.func.Action;
import ratpack.func.Function;
import ratpack.registry.Registry;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Creates a registry of objects derived from an event stream.
 * <p>
 * Facilitates composing a strategy for reducing/converting an event stream into different objects in one pass.
 * <p>
 * Processors are reusable and threadsafe.
 */
public final class EventStreamProcessor {

    private final List<EventStreamReducer<?>> reducers;

    private EventStreamProcessor(List<EventStreamReducer<?>> reducers) {
        this.reducers = reducers;
    }

    public static EventStreamProcessor of(Action<? super EventStreamProcessorSpec> action) throws Exception {
        ImmutableList.Builder<EventStreamReducer<?>> reducers = ImmutableList.builder();
        EventStreamProcessorSpec spec = new EventStreamProcessorSpec() {
            @Override
            public <T> EventStreamProcessorSpec add(TypeToken<T> reducedType, Function<? super EventStreamReducerBuilder<T>, ? extends EventStreamReducer<T>> spec) throws Exception {
                reducers.add(spec.apply(new EventStreamReducerBuilder<>(reducedType)));
                return this;
            }
        };
        action.execute(spec);
        return new EventStreamProcessor(reducers.build());
    }

    public static <T> Promise<T> reduce(Class<T> type, ExportBuild build, EventPublisherFactory eventPublisherFactory, EventStreamReductionTemplate<T> template) throws Exception {
        return of(s -> s.addTemplate(type, template)).process(build, eventPublisherFactory).map(r -> r.get(type));
    }

    public Promise<Registry> process(ExportBuild build, EventPublisherFactory eventPublisherFactory) throws Exception {
        return Promise.flatten(() -> {
            EventStreamProcessing eventStreamProcessing = createEventStreamProcessing(build);
            Set<String> eventTypes = eventStreamProcessing.getEventDataTypes().stream().map(VersionedEventType::of).map(e -> e.base).collect(Collectors.toSet());
            return eventPublisherFactory.get(eventTypes)
                .flatMap(eventStreamProcessing::process);
        });
    }

    private EventStreamProcessing createEventStreamProcessing(ExportBuild build) throws Exception {
        return new EventStreamProcessing(reducers, build);
    }

}
