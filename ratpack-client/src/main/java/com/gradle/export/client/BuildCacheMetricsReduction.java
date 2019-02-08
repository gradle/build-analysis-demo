package com.gradle.export.client;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import com.gradle.export.client.event.BuildEvent;
import com.gradle.export.client.event.ExportBuild;
import com.gradle.export.client.event.BuildEventReduction;
import com.gradle.export.client.event.processor.EventStreamReduction;
import com.gradle.export.client.event.processor.EventStreamReductionBuilder;
import com.gradle.export.client.util.EventMatcher;
import com.gradle.export.client.util.Ref;
import com.gradle.scan.eventmodel.BuildCachePackFinished_1_0;
import com.gradle.scan.eventmodel.BuildCachePackStarted_1_0;
import com.gradle.scan.eventmodel.BuildCacheRemoteLoadFinished_1_0;
import com.gradle.scan.eventmodel.BuildCacheRemoteLoadStarted_1_0;
import com.gradle.scan.eventmodel.BuildCacheRemoteStoreFinished_1_0;
import com.gradle.scan.eventmodel.BuildCacheRemoteStoreStarted_1_0;
import com.gradle.scan.eventmodel.BuildCacheUnpackFinished_1_0;
import com.gradle.scan.eventmodel.BuildCacheUnpackStarted_1_0;
import com.gradle.scan.eventmodel.Jvm_1_0;
import com.gradle.scan.eventmodel.Os_1_0;
import com.gradle.scan.eventmodel.ProjectStructure_1_0;
import com.gradle.scan.eventmodel.TaskFinished_1_0;
import com.gradle.scan.eventmodel.TaskFinished_1_2;
import com.gradle.scan.eventmodel.TaskFinished_1_5;
import com.gradle.scan.eventmodel.TaskStarted_1_0;
import com.gradle.scan.eventmodel.UserTag_1_0;
import com.gradle.scan.eventmodel.task.TaskOutcome_1;
import ratpack.exec.Blocking;
import ratpack.exec.Operation;
import ratpack.util.Exceptions;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.Charset;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import static com.google.common.base.MoreObjects.firstNonNull;
import static org.apache.commons.lang3.StringEscapeUtils.escapeCsv;

final class BuildCacheMetricsReduction implements BuildEventReduction<BuildCacheMetricsReduction.TaskInfoContainer> {

    private static final List<String> CSV_FIELDS = ImmutableList.of(
        "buildId",
        "buildDate",
        "gradleVersion",
        "javaVersion",
        "os",
        "pluginVersion",
        "rootProject",
        "serialBuildExecutionTime",
        "wallClockBuildExecutionTime",
        "path",
        "type",
        "outcome",
        "started",
        "finished",
        "remoteMissTime",
        "remoteHitTime",
        "remoteStoreTime",
        "unpackTime",
        "packTime",
        "artifactSize",
        "artifactEntries",
        "executionTime",
        "originExecutionTime",
        "isCi",
        "tags"
    );
    private final AtomicInteger queueSize = new AtomicInteger();
    private final AtomicInteger complete = new AtomicInteger();
    private final Writer writer;
    private final boolean hashIdentities;
    private HashFunction hasher;

    BuildCacheMetricsReduction(Writer writer, boolean hashIdentities) {
        this.writer = writer;
        this.hashIdentities = hashIdentities;

        if (hashIdentities) {
            this.hasher = Hashing.goodFastHash(32);
        }

        Exceptions.uncheck(() -> {
            // Write output file header
            writer.append(Joiner.on(',').join(CSV_FIELDS)).append('\n');
        });
    }

    @Override
    public Class<TaskInfoContainer> getType() {
        return TaskInfoContainer.class;
    }

    @Override
    public EventStreamReduction<TaskInfoContainer> build(EventStreamReductionBuilder<TaskInfoContainer> builder) throws Exception {
        Ref<Os_1_0> os = Ref.empty();
        Ref<Jvm_1_0> jvm = Ref.empty();
        Ref<ProjectStructure_1_0> projectStructure = Ref.empty();
        EventMatcher<BuildCacheRemoteLoadStarted_1_0> remoteLoadMatcher = new EventMatcher<>();
        EventMatcher<BuildCacheRemoteStoreStarted_1_0> remoteStoreMatcher = new EventMatcher<>();
        EventMatcher<BuildCachePackStarted_1_0> cachePackMatcher = new EventMatcher<>();
        EventMatcher<BuildCacheUnpackStarted_1_0> cacheUnpackMatcher = new EventMatcher<>();
        Map<Long, TaskInfo> taskInfoMap = new HashMap<>();
        List<TaskInfo> tasks = new ArrayList<>();
        List<String> tags = new ArrayList<>();
        Ref<BuildEvent<TaskStarted_1_0>> firstTaskStart = Ref.empty();
        Ref<BuildEvent<TaskFinished_1_0>> lastTaskFinish = Ref.empty();
        Ref<Duration> serialExecutionTime = Ref.of(Duration.ZERO);

        Function<Long, TaskInfo> getTask = id -> taskInfoMap.computeIfAbsent(id, l -> {
            TaskInfo taskInfo = new TaskInfo();
            taskInfo.buildId = builder.build.buildId;
            taskInfo.buildDate = builder.build.timestamp;
            taskInfo.gradleVersion = builder.build.gradleVersion;
            taskInfo.javaVersion = jvm.get().version;
            taskInfo.os = os.get().name + " " + os.get().version;
            taskInfo.pluginVersion = builder.build.pluginVersion;
            taskInfo.rootProject = maybeHash(projectStructure.get().rootProjectName);

            return taskInfo;
        });

        return builder
            .on(Os_1_0.class, e -> os.set(e.getData()))
            .on(Jvm_1_0.class, e -> jvm.set(e.getData()))
            .on(ProjectStructure_1_0.class, e -> projectStructure.set(e.getData()))
            .pairing(TaskStarted_1_0.class, e -> e.id, TaskFinished_1_0.class, e -> e.id, pair -> {
                TaskInfo taskInfo = Optional.ofNullable(taskInfoMap.remove(pair.start.getData().id)).orElseGet(() -> getTask.apply(pair.start.getData().id));
                serialExecutionTime.update(t -> t.plus(pair.getDuration()));
                
                if (firstTaskStart.isEmpty()) {
                    firstTaskStart.set(pair.start);
                }
                lastTaskFinish.set(pair.finish);
                
                if (pair.finish.getData() instanceof TaskFinished_1_2) {
                    if (((TaskFinished_1_2) pair.finish.getData()).cacheable && (pair.finish.getData().outcome == TaskOutcome_1.SUCCESS || pair.finish.getData().outcome == TaskOutcome_1.FAILED || pair.finish.getData().outcome == TaskOutcome_1.FROM_CACHE)) {
                        taskInfo.path = maybeHash(pair.start.getData().path);
                        taskInfo.type = maybeHash(pair.start.getData().className);
                        taskInfo.outcome = pair.finish.getData().outcome.name();
                        taskInfo.started = Optional.of(pair.start.getTimestamp());
                        taskInfo.finished = Optional.of(pair.finish.getTimestamp());
                        taskInfo.executionTime = pair.getDuration();
                        if (pair.finish.getData() instanceof TaskFinished_1_5) {
                            Long originExecutionTime = ((TaskFinished_1_5) pair.finish.getData()).originExecutionTime;
                            taskInfo.originExecutionTime = Duration.ofMillis(originExecutionTime == null ? 0 : originExecutionTime);
                        }
                        tasks.add(taskInfo);
                    }
                }
            })
            .onOptionallySupported(BuildCacheRemoteLoadStarted_1_0.class, e -> remoteLoadMatcher.open(e.getData().id, e))
            .onOptionallySupported(BuildCacheRemoteLoadFinished_1_0.class, finish -> {
                BuildEvent<BuildCacheRemoteLoadStarted_1_0> start = remoteLoadMatcher.close(finish.getData().id);
                Duration duration = Duration.between(start.getTimestamp(), finish.getTimestamp());
                TaskInfo taskInfo = getTask.apply(start.getData().task);
                if (firstNonNull(finish.getData().hit, false)) {
                    taskInfo.remoteHitTime = duration;
                } else {
                    taskInfo.remoteMissTime = duration;
                }
            })
            .onOptionallySupported(BuildCacheRemoteStoreStarted_1_0.class, e -> remoteStoreMatcher.open(e.getData().id, e))
            .onOptionallySupported(BuildCacheRemoteStoreFinished_1_0.class, finish -> {
                BuildEvent<BuildCacheRemoteStoreStarted_1_0> start = remoteStoreMatcher.close(finish.getData().id);
                getTask.apply(start.getData().task).remoteStoreTime = Duration.between(start.getTimestamp(), finish.getTimestamp());
            })
            .onOptionallySupported(BuildCachePackStarted_1_0.class, e -> cachePackMatcher.open(e.getData().id, e))
            .onOptionallySupported(BuildCachePackFinished_1_0.class, finish -> {
                BuildEvent<BuildCachePackStarted_1_0> start = cachePackMatcher.close(finish.getData().id);
                TaskInfo taskInfo = getTask.apply(start.getData().task);
                taskInfo.packTime = Duration.between(start.getTimestamp(), finish.getTimestamp());
                taskInfo.artifactSize = firstNonNull(finish.getData().archiveSize, 0L);
                taskInfo.artifactEntries = firstNonNull(finish.getData().archiveEntryCount, 0L);
            })
            .onOptionallySupported(BuildCacheUnpackStarted_1_0.class, e -> cacheUnpackMatcher.open(e.getData().id, e))
            .onOptionallySupported(BuildCacheUnpackFinished_1_0.class, finish -> {
                BuildEvent<BuildCacheUnpackStarted_1_0> start = cacheUnpackMatcher.close(finish.getData().id);
                TaskInfo taskInfo = getTask.apply(start.getData().task);
                taskInfo.unpackTime = Duration.between(start.getTimestamp(), finish.getTimestamp());
                taskInfo.artifactSize = start.getData().archiveSize;
                taskInfo.artifactEntries = firstNonNull(finish.getData().archiveEntryCount, 0L);
            })
            .onOptionallySupported(UserTag_1_0.class, e -> tags.add(e.getData().tag))
            .then(r -> new TaskInfoContainer(builder.build, tasks, tags, serialExecutionTime.get(), !firstTaskStart.isEmpty() && !lastTaskFinish.isEmpty() ? Duration.between(firstTaskStart.get().getTimestamp(), lastTaskFinish.get().getTimestamp()) : Duration.ZERO));
    }

    @Override
    public Operation complete(TaskInfoContainer result) throws Exception {
        System.out.print("Processed " + complete.incrementAndGet() + " of " + queueSize.get() + " builds\r");
        boolean isCi = Iterables.any(result.tags, t -> t.toLowerCase().equals("ci"));
        Iterable<String> tags = ImmutableList.copyOf(Lists.transform(result.tags, this::maybeHash));

        return Blocking.op(() -> {
            synchronized (writer) {
                for (TaskInfo task : result.tasks) {
                    try {
                        writeTask(task, isCi, tags, result.serialBuildExecutionTime, result.wallClockBuildExecutionTime);
                    } catch (Exception e) {
                        throw new IllegalStateException("Failed to write task " + task.path, e);
                    }
                }
            }
        });
    }

    private void writeTask(TaskInfo task, boolean isCi, Iterable<String> tags, Duration serialExecutionTime, Duration wallClockExecutionTime) throws IOException {
        writer.append(escapeCsv(task.buildId))
            .append(',')
            .append(escapeCsv(task.buildDate.toString()))
            .append(',')
            .append(escapeCsv(task.gradleVersion))
            .append(',')
            .append(escapeCsv(task.javaVersion))
            .append(',')
            .append(escapeCsv(task.os))
            .append(',')
            .append(escapeCsv(task.pluginVersion))
            .append(',')
            .append(escapeCsv(task.rootProject))
            .append(',')
            .append(Long.toString(serialExecutionTime.toMillis()))
            .append(',')
            .append(Long.toString(wallClockExecutionTime.toMillis()))
            .append(',')
            .append(escapeCsv(task.path))
            .append(',')
            .append(escapeCsv(task.type))
            .append(',')
            .append(task.outcome)
            .append(',')
            .append(task.started.map(Instant::toString).orElse(""))
            .append(',')
            .append(task.finished.map(Instant::toString).orElse(""))
            .append(',')
            .append(Long.toString(task.remoteMissTime.toMillis()))
            .append(',')
            .append(Long.toString(task.remoteHitTime.toMillis()))
            .append(',')
            .append(Long.toString(task.remoteStoreTime.toMillis()))
            .append(',')
            .append(Long.toString(task.unpackTime.toMillis()))
            .append(',')
            .append(Long.toString(task.packTime.toMillis()))
            .append(',')
            .append(Long.toString(task.artifactSize))
            .append(',')
            .append(Long.toString(task.artifactEntries))
            .append(',')
            .append(Long.toString(task.executionTime.toMillis()))
            .append(',')
            .append(Long.toString(task.originExecutionTime.toMillis()))
            .append(',')
            .append(Boolean.toString(isCi))
            .append(',')
            .append(escapeCsv("{" + Joiner.on(',').join(tags) + "}"))
            .append('\n');
    }

    private String maybeHash(String value) {
        if (hashIdentities) {
            return hasher.newHasher()
                .putString(value, Charset.defaultCharset())
                .hash()
                .toString();
        }

        return value;
    }

    @Override
    public void queued() {
        queueSize.getAndIncrement();
    }

    static class TaskInfo {
        String buildId;
        Instant buildDate;
        String gradleVersion;
        String javaVersion;
        String os;
        String pluginVersion;
        String rootProject;
        String path;
        String type;
        String outcome;
        Optional<Instant> started = Optional.empty();
        Optional<Instant> finished = Optional.empty();
        Duration remoteMissTime = Duration.ZERO;
        Duration remoteHitTime = Duration.ZERO;
        Duration remoteStoreTime = Duration.ZERO;
        Duration unpackTime = Duration.ZERO;
        Duration packTime = Duration.ZERO;
        long artifactSize;
        long artifactEntries;
        Duration executionTime = Duration.ZERO;
        Duration originExecutionTime = Duration.ZERO;
    }

    static class TaskInfoContainer {

        final ExportBuild build;
        final Iterable<TaskInfo> tasks;
        final List<String> tags;
        final Duration serialBuildExecutionTime;
        final Duration wallClockBuildExecutionTime;

        TaskInfoContainer(ExportBuild build, Iterable<TaskInfo> tasks, List<String> tags, Duration serialBuildExecutionTime, Duration wallClockBuildExecutionTime) {
            this.build = build;
            this.tasks = tasks;
            this.tags = tags;
            this.serialBuildExecutionTime = serialBuildExecutionTime;
            this.wallClockBuildExecutionTime = wallClockBuildExecutionTime;
        }
    }
}
