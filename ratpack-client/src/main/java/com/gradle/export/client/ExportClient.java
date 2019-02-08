package com.gradle.export.client;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import com.google.common.base.Joiner;
import com.gradle.export.client.event.BuildEvent;
import com.gradle.export.client.event.ExportBuild;
import com.gradle.export.client.event.VersionedEventType;
import com.gradle.export.client.event.BuildEventReduction;
import com.gradle.export.client.event.processor.EventPublisherFactory;
import com.gradle.export.client.event.processor.EventStreamProcessor;
import com.gradle.export.client.util.LowerCaseEnumJacksonModule;
import com.gradle.scan.eventmodel.EventData;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ratpack.exec.Execution;
import ratpack.exec.Operation;
import ratpack.exec.Promise;
import ratpack.exec.Throttle;
import ratpack.func.Action;
import ratpack.http.client.HttpClient;
import ratpack.http.client.HttpClientReadTimeoutException;
import ratpack.sse.Event;
import ratpack.sse.internal.ServerSentEventDecodingPublisher;
import ratpack.stream.Streams;
import ratpack.stream.TransformablePublisher;
import ratpack.util.Exceptions;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;

final class ExportClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExportClient.class);
    private static final int MAX_FORKED_EXECUTIONS = 100;
    private static final ObjectMapper MAPPER = new ObjectMapper()
        .disable(DeserializationFeature.READ_DATE_TIMESTAMPS_AS_NANOSECONDS)
        .registerModule(new Jdk8Module())
        .registerModule(new JavaTimeModule())
        .registerModule(new ParameterNamesModule())
        .registerModule(new LowerCaseEnumJacksonModule());

    private final Throttle eventStreamThrottle;

    private final ExportClientSpec spec;
    private final HttpClient httpClient;

    ExportClient(ExportClientSpec spec) throws Exception {
        this.spec = spec;
        this.httpClient = HttpClient.of(s -> s.poolSize(30));
        this.eventStreamThrottle = Throttle.ofSize(spec.workers);
    }

    Operation export(BuildEventReduction<?> reduction) {
        return buildStreamSinceInstant(Instant.now().minus(Duration.ofDays(spec.days)))
            .onYield(() -> LOGGER.info("Starting build scan data export"))
            .map(stream -> Streams.wiretap(stream, e -> {
                if (e.isData()) {
                    reduction.queued();
                }
            }))
            // fork this stream so we can eagerly get a count of how many builds need processing as well as terminate build stream request early
            .map(stream -> Streams.fork(stream, Action.noop(), Action.noop()))
            .flatMap(stream -> Promise.async(down -> stream.subscribe(new Subscriber<ExportBuild>() {
                private Subscription subscription;
                private AtomicInteger remaining = new AtomicInteger();
                private volatile boolean complete = false;
                private volatile Throwable error = null;

                @Override
                public void onSubscribe(Subscription s) {
                    this.subscription = s;
                    s.request(MAX_FORKED_EXECUTIONS);
                }

                @Override
                public void onNext(ExportBuild exportBuild) {
                    remaining.getAndIncrement();
                    if (error != null) {
                        if (remaining.decrementAndGet() == 0) {
                            stop();
                        }
                    } else {
                        Execution.fork()
                            .onError(e -> {
                                subscription.cancel();
                                error = new IllegalStateException("Failed to process build " + exportBuild.buildId, e);
                            })
                            .onComplete(e -> {
                                if (remaining.decrementAndGet() == 0 && (complete || error != null)) {
                                    stop();
                                } else if (!complete) {
                                    subscription.request(1);
                                }
                            })
                            .start(
                                consumeBuild(exportBuild, reduction)
                                    .throttled(eventStreamThrottle)
                                    .retry(3, Duration.ofSeconds(5), (i, t) -> {
                                        if (t instanceof IOException || t instanceof HttpClientReadTimeoutException) {
                                            LOGGER.error("Processing of build {} failed (attempt {} of 3): {}", exportBuild.buildId, i, t.getMessage());
                                        } else {
                                            throw Exceptions.uncheck(t);
                                        }
                                    })
                                    .operation()
                            );
                    }
                }

                @Override
                public void onError(Throwable t) {
                    error = t;
                    maybeStop();
                }

                @Override
                public void onComplete() {
                    complete = true;
                    maybeStop();
                }

                private void maybeStop() {
                    if (remaining.get() == 0) {
                        stop();
                    }
                }

                private void stop() {
                    if (error == null) {
                        down.success(null);
                    } else {
                        down.error(error);
                    }
                }
            })))
            .timeResult((result, duration) -> {
                if (result.isSuccess()) {
                    LOGGER.info("Completed in {}", duration);
                }
            })
            .close(httpClient)
            .operation();
    }

    private <T> Promise<T> consumeBuild(ExportBuild exportBuild, BuildEventReduction<T> reduction) {
        return Promise.flatten(() ->
            EventStreamProcessor.reduce(reduction.getType(), exportBuild, buildEventStreamFactory(exportBuild.buildId), reduction)
                .nextOp(reduction::complete)
        );
    }

    private EventPublisherFactory buildEventStreamFactory(String buildId) {
        return eventTypes -> {
            URI requestUri = spec.serverUri.resolve("/build-export/v1/build/" + buildId + "/events?eventTypes=" + Joiner.on(',').join(eventTypes));
            return openStream(requestUri)
                .map(Streams::bindExec)
                .map(events -> Streams.filter(events, event -> event.getEvent().equals("BuildEvent")))
                .map(events -> Streams.map(events, this::parseBuildEvent));
        };
    }

    private Promise<TransformablePublisher<ExportBuild>> buildStreamSinceInstant(Instant since) {
        return openStream(spec.serverUri.resolve("/build-export/v1/builds/since/" + since.toEpochMilli()))
            .map(events -> events.map(ExportClient::parseExportBuild));
    }

    private Promise<TransformablePublisher<Event<?>>> openStream(URI uri) {
        return httpClient.requestStream(uri, s -> {
            if (spec.username != null && spec.password != null) {
                s.basicAuth(spec.username, spec.password);
            }
            s.readTimeout(Duration.ofSeconds(10));
            s.getHeaders().set(HttpHeaderNames.ACCEPT_ENCODING, HttpHeaderValues.GZIP_DEFLATE);
        })
            .map(r -> {
                if (r.getStatusCode() == 200) {
                    return new ServerSentEventDecodingPublisher(r.getBody(), httpClient.getByteBufAllocator());
                } else if (r.getStatusCode() == 401) {
                    throw new IllegalStateException("Server requires username and password credentials");
                } else {
                    throw new IllegalStateException("Unexpected response from server: " + r.getStatusCode());
                }
            });
    }

    private BuildEvent<?> parseBuildEvent(Event<?> event) throws IOException {
        ExportEvent exportEvent = MAPPER.readValue(event.getData(), ExportEvent.class);
        VersionedEventType eventType = VersionedEventType.of(exportEvent.type.eventType, exportEvent.type.majorVersion, exportEvent.type.minorVersion);
        Class<? extends EventData> eventClass = eventType.getClazz();
        EventData eventData = MAPPER.convertValue(exportEvent.data, eventClass);
        return new BuildEvent<>(eventData, eventType, exportEvent.timestamp);
    }

    private static ExportBuild parseExportBuild(Event<?> event) throws IOException {
        return MAPPER.readValue(event.getData(), ExportBuild.class);
    }

    private static class ExportEvent {

        public Instant timestamp;
        public Type type;
        public JsonNode data;

        public static class Type {
            public String eventType;
            public short majorVersion;
            public short minorVersion;

        }
    }

}
