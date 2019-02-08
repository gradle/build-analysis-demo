package com.gradle.export.client.event;

import com.gradle.scan.eventmodel.EventData;
import ratpack.util.Types;

import java.time.Instant;

public class BuildEvent<T extends EventData>  {
    private final T data;
    private final VersionedEventType type;
    private final Instant timestamp;

    public BuildEvent(T data, VersionedEventType type, Instant timestamp) {
        this.data = data;
        this.type = type;
        this.timestamp = timestamp;
    }

    public <O extends EventData> BuildEvent<O> cast(Class<O> type) {
        if (!type.isAssignableFrom(this.type.getClazz())) {
            throw new ClassCastException("Cannot cast event with data type " + this.type.getClazz().getName() + " to " + type.getName());
        }

        return Types.cast(this);
    }

    public T getData() {
        return data;
    }

    public VersionedEventType getType() {
        return type;
    }

    public Instant getTimestamp() {
        return timestamp;
    }
}
