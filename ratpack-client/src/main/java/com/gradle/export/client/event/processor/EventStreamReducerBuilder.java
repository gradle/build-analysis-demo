package com.gradle.export.client.event.processor;

import com.google.common.collect.ImmutableSet;
import com.google.common.reflect.TypeToken;
import com.gradle.export.client.event.ExportBuild;
import com.gradle.export.client.version.GradleVersions;
import com.gradle.export.client.version.PluginVersions;
import com.gradle.scan.eventmodel.EventData;

import java.util.Collections;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;

public final class EventStreamReducerBuilder<T> {

    private final TypeToken<T> type;

    private Set<Class<?>> dependencies = Collections.emptySet();
    private BiFunction<? super ExportBuild, ? super Set<Class<? extends EventData>>, Boolean> onlyIf = (b, e) -> GradleVersions.isCaptured(e, GradleVersions.parse(b.gradleVersion))
        && PluginVersions.isCaptured(e, PluginVersions.parse(b.pluginVersion));

    EventStreamReducerBuilder(TypeToken<T> type) {
        this.type = type;
    }

    /**
     * Declares that this reducer depends on the given reduction types, expecting upstream reducers to provide them.
     * <p>
     * It is guaranteed that those reducers which create reductions of the given types are invoked upstream and
     * the reduction results made available in the registry function given to {@link EventStreamReductionBuilder#then(Function)}.
     */
    public EventStreamReducerBuilder<T> dependencies(Iterable<Class<?>> dependencies) {
        this.dependencies = ImmutableSet.copyOf(dependencies);
        return this;
    }

    /**
     * Declares that this reducer must only be applied if the given condition is satisfied.
     */
    public EventStreamReducerBuilder<T> onlyIf(BiFunction<? super ExportBuild, ? super Set<Class<? extends EventData>>, Boolean> onlyIf) {
        this.onlyIf = onlyIf;
        return this;
    }

    public EventStreamReducer<T> template(EventStreamReductionTemplate<T> template) {
        return new EventStreamReducer<>(type, dependencies, onlyIf, template);
    }

}
