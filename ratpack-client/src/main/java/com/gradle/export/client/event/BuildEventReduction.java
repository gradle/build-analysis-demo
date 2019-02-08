package com.gradle.export.client.event;

import com.gradle.export.client.event.processor.EventStreamReductionTemplate;
import ratpack.exec.Operation;

public interface BuildEventReduction<T> extends EventStreamReductionTemplate<T> {

    Class<T> getType();

    default Operation complete(T result) throws Exception {
        return Operation.noop();
    }

    default void queued() {
        // do nothing
    }
}
