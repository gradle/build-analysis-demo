package com.gradle.export.client.event.processor;

import com.google.common.reflect.TypeToken;
import com.gradle.export.client.event.ExportBuild;
import com.gradle.scan.eventmodel.EventData;

import java.util.Set;
import java.util.function.BiFunction;

@SuppressWarnings("WeakerAccess") // needs to be implemented out of package as lambda, which IDEA doesn't understand
public final class EventStreamReducer<T> {

    final TypeToken<T> type;
    final Set<Class<?>> dependencies;
    final BiFunction<? super ExportBuild, ? super Set<Class<? extends EventData>>, Boolean> onlyIf;
    final EventStreamReductionTemplate<T> template;

    EventStreamReducer(
        TypeToken<T> type,
        Set<Class<?>> dependencies,
        BiFunction<? super ExportBuild, ? super Set<Class<? extends EventData>>, Boolean> onlyIf,
        EventStreamReductionTemplate<T> template
    ) {
        this.type = type;
        this.dependencies = dependencies;
        this.onlyIf = onlyIf;
        this.template = template;
    }

}
