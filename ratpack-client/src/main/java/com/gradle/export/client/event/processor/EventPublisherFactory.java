package com.gradle.export.client.event.processor;

import com.gradle.export.client.event.BuildEvent;
import org.reactivestreams.Publisher;
import ratpack.exec.Promise;

import java.util.Set;

public interface EventPublisherFactory {
    Promise<Publisher<BuildEvent<?>>> get(Set<String> eventTypes);
}
