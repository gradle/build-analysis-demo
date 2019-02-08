package com.gradle.export.client.event.processor;

import com.google.common.reflect.TypeToken;
import ratpack.func.Function;

public interface EventStreamProcessorSpec {

    <T> EventStreamProcessorSpec add(TypeToken<T> reducedType, Function<? super EventStreamReducerBuilder<T>, ? extends EventStreamReducer<T>> spec) throws Exception;

    default <T> EventStreamProcessorSpec add(Class<T> reducedType, Function<? super EventStreamReducerBuilder<T>, ? extends EventStreamReducer<T>> spec) throws Exception {
        return add(TypeToken.of(reducedType), spec);
    }

    default <T> EventStreamProcessorSpec addTemplate(TypeToken<T> reducedType, EventStreamReductionTemplate<T> template) throws Exception {
        return add(reducedType, s -> s.template(template));
    }

    default <T> EventStreamProcessorSpec addTemplate(Class<T> reducedType, EventStreamReductionTemplate<T> template) throws Exception {
        return add(TypeToken.of(reducedType), s -> s.template(template));
    }

}
