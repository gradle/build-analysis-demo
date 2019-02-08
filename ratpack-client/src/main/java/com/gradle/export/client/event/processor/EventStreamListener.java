package com.gradle.export.client.event.processor;

public interface EventStreamListener {

    void addListeners(EventStreamReductionBuilder<?> b);

}
