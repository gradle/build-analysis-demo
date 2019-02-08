package com.gradle.export.client.event;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Lists;
import com.gradle.scan.eventmodel.BasicMemoryStats_1_0;
import com.gradle.scan.eventmodel.BasicMemoryStats_1_1;
import com.gradle.scan.eventmodel.BuildCachePackFinished_1_0;
import com.gradle.scan.eventmodel.BuildCachePackFinished_1_1;
import com.gradle.scan.eventmodel.BuildCacheRemoteLoadFinished_1_0;
import com.gradle.scan.eventmodel.BuildCacheRemoteLoadFinished_1_1;
import com.gradle.scan.eventmodel.BuildCacheRemoteStoreFinished_1_0;
import com.gradle.scan.eventmodel.BuildCacheRemoteStoreFinished_1_1;
import com.gradle.scan.eventmodel.BuildCacheUnpackFinished_1_0;
import com.gradle.scan.eventmodel.BuildCacheUnpackFinished_1_1;
import com.gradle.scan.eventmodel.BuildFinished_1_0;
import com.gradle.scan.eventmodel.BuildFinished_1_1;
import com.gradle.scan.eventmodel.BuildModes_1_0;
import com.gradle.scan.eventmodel.BuildModes_1_1;
import com.gradle.scan.eventmodel.ConfigurationResolutionData_1_0;
import com.gradle.scan.eventmodel.ConfigurationResolutionData_1_1;
import com.gradle.scan.eventmodel.ConfigurationResolutionData_1_2;
import com.gradle.scan.eventmodel.ConfigurationResolutionStarted_1_0;
import com.gradle.scan.eventmodel.ConfigurationResolutionStarted_1_1;
import com.gradle.scan.eventmodel.ConfigurationResolutionStarted_1_2;
import com.gradle.scan.eventmodel.DaemonState_1_0;
import com.gradle.scan.eventmodel.DaemonState_1_1;
import com.gradle.scan.eventmodel.EventData;
import com.gradle.scan.eventmodel.ExceptionData_1_0;
import com.gradle.scan.eventmodel.ExceptionData_1_1;
import com.gradle.scan.eventmodel.NetworkDownloadActivityFinished_1_0;
import com.gradle.scan.eventmodel.NetworkDownloadActivityFinished_1_1;
import com.gradle.scan.eventmodel.NetworkDownloadActivityStarted_1_0;
import com.gradle.scan.eventmodel.NetworkDownloadActivityStarted_1_1;
import com.gradle.scan.eventmodel.OutputLogEvent_1_0;
import com.gradle.scan.eventmodel.OutputLogEvent_1_1;
import com.gradle.scan.eventmodel.OutputLogEvent_1_2;
import com.gradle.scan.eventmodel.OutputStyledTextEvent_1_0;
import com.gradle.scan.eventmodel.OutputStyledTextEvent_1_1;
import com.gradle.scan.eventmodel.ProjectEvaluationFinished_1_0;
import com.gradle.scan.eventmodel.ProjectEvaluationFinished_1_1;
import com.gradle.scan.eventmodel.ProjectEvaluationFinished_1_2;
import com.gradle.scan.eventmodel.ProjectEvaluationStarted_1_0;
import com.gradle.scan.eventmodel.ProjectEvaluationStarted_1_1;
import com.gradle.scan.eventmodel.ProjectStructure_1_0;
import com.gradle.scan.eventmodel.ProjectStructure_1_1;
import com.gradle.scan.eventmodel.ProjectStructure_1_2;
import com.gradle.scan.eventmodel.TaskFinished_1_0;
import com.gradle.scan.eventmodel.TaskFinished_1_1;
import com.gradle.scan.eventmodel.TaskFinished_1_2;
import com.gradle.scan.eventmodel.TaskFinished_1_3;
import com.gradle.scan.eventmodel.TaskFinished_1_4;
import com.gradle.scan.eventmodel.TaskFinished_1_5;
import com.gradle.scan.eventmodel.TaskFinished_1_6;
import com.gradle.scan.eventmodel.TaskGraphCalculationFinished_1_0;
import com.gradle.scan.eventmodel.TaskGraphCalculationFinished_1_1;
import com.gradle.scan.eventmodel.TaskGraphCalculationStarted_1_0;
import com.gradle.scan.eventmodel.TaskGraphCalculationStarted_1_1;
import com.gradle.scan.eventmodel.TaskStarted_1_0;
import com.gradle.scan.eventmodel.TaskStarted_1_1;
import com.gradle.scan.eventmodel.TaskStarted_1_2;
import com.gradle.scan.eventmodel.TaskStarted_1_3;
import com.gradle.scan.eventmodel.TaskStarted_1_4;
import com.gradle.scan.eventmodel.TaskStarted_1_5;
import com.gradle.scan.eventmodel.TestFinished_1_0;
import com.gradle.scan.eventmodel.TestFinished_1_1;

import java.util.List;

@VisibleForTesting
public final class EventDataHierarchy {

    @VisibleForTesting
    public static final ImmutableMultimap<Class<? extends EventData>, Class<? extends EventData>> HIERARCHY = populateHierarchy();

    private static ImmutableMultimap<Class<? extends EventData>, Class<? extends EventData>> populateHierarchy() {
        ImmutableMultimap.Builder<Class<? extends EventData>, Class<? extends EventData>> builder = ImmutableMultimap.builder();
        populate(Lists.newArrayList(
            BuildModes_1_0.class,
            BuildModes_1_1.class
        ), builder);
        populate(Lists.newArrayList(
            BasicMemoryStats_1_0.class,
            BasicMemoryStats_1_1.class
        ), builder);
        populate(Lists.newArrayList(
            ProjectStructure_1_0.class,
            ProjectStructure_1_1.class,
            ProjectStructure_1_2.class
        ), builder);
        populate(Lists.newArrayList(
            TaskStarted_1_0.class,
            TaskStarted_1_1.class,
            TaskStarted_1_2.class,
            TaskStarted_1_3.class,
            TaskStarted_1_4.class,
            TaskStarted_1_5.class
        ), builder);
        populate(Lists.newArrayList(
            TaskFinished_1_0.class,
            TaskFinished_1_1.class,
            TaskFinished_1_2.class,
            TaskFinished_1_3.class,
            TaskFinished_1_4.class,
            TaskFinished_1_5.class,
            TaskFinished_1_6.class
        ), builder);
        populate(Lists.newArrayList(
            NetworkDownloadActivityStarted_1_0.class,
            NetworkDownloadActivityStarted_1_1.class
        ), builder);
        populate(Lists.newArrayList(
            NetworkDownloadActivityFinished_1_0.class,
            NetworkDownloadActivityFinished_1_1.class
        ), builder);
        populate(Lists.newArrayList(
            DaemonState_1_0.class,
            DaemonState_1_1.class
        ), builder);
        populate(Lists.newArrayList(
            ConfigurationResolutionStarted_1_0.class,
            ConfigurationResolutionStarted_1_1.class,
            ConfigurationResolutionStarted_1_2.class
        ), builder);
        populate(Lists.newArrayList(
            ProjectEvaluationStarted_1_0.class,
            ProjectEvaluationStarted_1_1.class
        ), builder);
        populate(Lists.newArrayList(
            ProjectEvaluationFinished_1_0.class,
            ProjectEvaluationFinished_1_1.class,
            ProjectEvaluationFinished_1_2.class
        ), builder);
        populate(Lists.newArrayList(
            TaskGraphCalculationStarted_1_0.class,
            TaskGraphCalculationStarted_1_1.class
        ), builder);
        populate(Lists.newArrayList(
            TaskGraphCalculationFinished_1_0.class,
            TaskGraphCalculationFinished_1_1.class
        ), builder);
        populate(Lists.newArrayList(
            OutputLogEvent_1_0.class,
            OutputLogEvent_1_1.class,
            OutputLogEvent_1_2.class
        ), builder);
        populate(Lists.newArrayList(
            OutputStyledTextEvent_1_0.class,
            OutputStyledTextEvent_1_1.class
        ), builder);
        populate(Lists.newArrayList(
            TestFinished_1_0.class,
            TestFinished_1_1.class
        ), builder);
        populate(Lists.newArrayList(
            BuildFinished_1_0.class,
            BuildFinished_1_1.class
        ), builder);
        populate(Lists.newArrayList(
            BuildCachePackFinished_1_0.class,
            BuildCachePackFinished_1_1.class
        ), builder);
        populate(Lists.newArrayList(
            BuildCacheUnpackFinished_1_0.class,
            BuildCacheUnpackFinished_1_1.class
        ), builder);
        populate(Lists.newArrayList(
            BuildCacheRemoteLoadFinished_1_0.class,
            BuildCacheRemoteLoadFinished_1_1.class
        ), builder);
        populate(Lists.newArrayList(
            BuildCacheRemoteStoreFinished_1_0.class,
            BuildCacheRemoteStoreFinished_1_1.class
        ), builder);
        populate(Lists.newArrayList(
            ConfigurationResolutionData_1_0.class,
            ConfigurationResolutionData_1_1.class,
            ConfigurationResolutionData_1_2.class
        ), builder);
        populate(Lists.newArrayList(
            ExceptionData_1_0.class,
            ExceptionData_1_1.class
        ), builder);
        return builder.build();
    }

    private static void populate(List<Class<? extends EventData>> hierarchy, ImmutableMultimap.Builder<Class<? extends EventData>, Class<? extends EventData>> builder) {
        if (hierarchy.size() > 1) {
            Class<? extends EventData> parent = hierarchy.remove(0);
            hierarchy.forEach(child -> builder.put(parent, child));
            populate(hierarchy, builder);
        }
    }

    private EventDataHierarchy() {
    }

}
