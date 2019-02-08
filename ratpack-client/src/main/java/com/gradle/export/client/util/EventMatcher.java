package com.gradle.export.client.util;

import com.gradle.export.client.event.BuildEvent;
import com.gradle.scan.eventmodel.EventData;

import java.util.HashMap;
import java.util.Map;

public class EventMatcher<T extends EventData> {

    private final Map<Long, BuildEvent<T>> startedEvents = new HashMap<>();

    public void open(long id, BuildEvent<T> openEvent) {
        BuildEvent<T> previous = startedEvents.put(id, openEvent);
        if (previous != null) {
            throw new IllegalStateException("Event already open for id " + id + ": " + previous + " (new: " + openEvent + ")");
        }
    }

    public BuildEvent<T> retrieve(long id) {
        return startedEvents.get(id);
    }

    public BuildEvent<T> close(long id) {
        return startedEvents.remove(id);
    }

}
