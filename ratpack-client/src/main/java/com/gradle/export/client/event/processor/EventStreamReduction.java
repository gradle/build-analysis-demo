package com.gradle.export.client.event.processor;

import com.google.common.collect.Multimap;
import com.google.common.reflect.TypeToken;
import com.gradle.scan.eventmodel.EventData;
import ratpack.exec.Promise;
import ratpack.registry.Registry;

import java.util.Set;
import java.util.function.Function;

public final class EventStreamReduction<T> {

    final TypeToken<T> type;
    final Set<Class<?>> dependencies;
    final Multimap<Class<? extends EventData>, EventHandler<?>> handlers;
    final Multimap<Class<? extends EventData>, EventHandler<?>> handlersOfOptionallySupportedEvents;
    final Function<? super Registry, ? extends Promise<T>> factory;

    EventStreamReduction(
        TypeToken<T> type,
        Set<Class<?>> dependencies,
        Multimap<Class<? extends EventData>, EventHandler<?>> handlers,
        Multimap<Class<? extends EventData>, EventHandler<?>> handlersOfOptionallySupportedEvents,
        Function<? super Registry, ? extends Promise<T>> factory
    ) {
        this.type = type;
        this.dependencies = dependencies;
        this.handlers = handlers;
        this.handlersOfOptionallySupportedEvents = handlersOfOptionallySupportedEvents;
        this.factory = factory;
    }

}
