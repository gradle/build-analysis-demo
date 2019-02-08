package com.gradle.export.client.event.processor;

import com.gradle.export.client.event.BuildEvent;
import com.gradle.scan.eventmodel.EventData;

public interface EventHandler<T extends EventData> {

    void handle(BuildEvent<T> event);

}
