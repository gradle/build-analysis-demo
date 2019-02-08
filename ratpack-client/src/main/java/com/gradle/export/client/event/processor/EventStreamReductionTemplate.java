package com.gradle.export.client.event.processor;

public interface EventStreamReductionTemplate<T> {

    EventStreamReduction<T> build(EventStreamReductionBuilder<T> builder) throws Exception;

}
