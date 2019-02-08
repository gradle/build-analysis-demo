package com.gradle.export.client.event;

import com.gradle.scan.eventmodel.EventData;

import java.time.Duration;

public final class PairedEvents<S extends EventData, F extends EventData> {

    public final BuildEvent<S> start;
    public final BuildEvent<F> finish;

    public PairedEvents(BuildEvent<S> start, BuildEvent<F> finish) {
        this.start = start;
        this.finish = finish;
    }

    public Duration getDuration() {
        return Duration.between(start.getTimestamp(), finish.getTimestamp());
    }

}
