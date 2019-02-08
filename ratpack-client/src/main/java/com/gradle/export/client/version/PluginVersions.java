package com.gradle.export.client.version;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.google.common.base.Preconditions;
import com.google.common.collect.Iterables;
import com.gradle.scan.eventmodel.EventData;
import ratpack.func.Pair;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Set;

import static com.gradle.export.client.event.EventDataHierarchy.HIERARCHY;

public final class PluginVersions {

    private static final LoadingCache<Pair<Class<? extends EventData>, PluginVersion>, Boolean> MATCH_FOR_EVENT_DATA_CLASS_PLUGIN_VERSION_PAIR = Caffeine.newBuilder()
        .maximumSize(10000).build(PluginVersions::isEventDataTypeCaptured);

    private static final LoadingCache<Pair<Set<Class<? extends EventData>>, PluginVersion>, Boolean> MATCH_FOR_EVENT_DATA_CLASSES_PLUGIN_VERSION_PAIR = Caffeine.newBuilder()
        .maximumSize(10000).build(PluginVersions::isEventDataTypesCaptured);

    private static final LoadingCache<String, PluginVersion> PLUGIN_VERSION_FOR_VERSION_PATTERN = Caffeine.newBuilder()
        .maximumSize(1000).build(PluginVersion::new);

    public static boolean isCaptured(Set<Class<? extends EventData>> eventDataTypes, PluginVersion pluginVersion) {
        return MATCH_FOR_EVENT_DATA_CLASSES_PLUGIN_VERSION_PAIR.get(Pair.of(eventDataTypes, pluginVersion));
    }

    private static boolean isEventDataTypesCaptured(Pair<Set<Class<? extends EventData>>, PluginVersion> pair) {
        Set<Class<? extends EventData>> eventDataTypes = pair.left;
        PluginVersion pluginVersion = pair.right;
        return Iterables.all(eventDataTypes, e -> MATCH_FOR_EVENT_DATA_CLASS_PLUGIN_VERSION_PAIR.get(Pair.of(e, pluginVersion)));
    }

    private static boolean isEventDataTypeCaptured(Pair<Class<? extends EventData>, PluginVersion> pair) {
        Class<? extends EventData> eventDataType = pair.left;
        PluginVersion pluginVersion = pair.right;
        return isCapturedByPlugin(eventDataType, pluginVersion) || isSubClassCapturedByPlugin(eventDataType, pluginVersion);
    }

    public static boolean isCapturedByPlugin(Class<? extends EventData> eventDataType, PluginVersion pluginVersion) {
        return PluginVersionRange.from(eventDataType).isInRange(pluginVersion);
    }

    private static boolean isSubClassCapturedByPlugin(Class<? extends EventData> eventDataType, PluginVersion pluginVersion) {
        Collection<Class<? extends EventData>> hierarchy = HIERARCHY.get(eventDataType);
        return hierarchy != null && Iterables.tryFind(hierarchy, child -> isCapturedByPlugin(child, pluginVersion)).isPresent();
    }

    public static PluginVersion parse(String version) {
        return PLUGIN_VERSION_FOR_VERSION_PATTERN.get(version);
    }

    private static final class PluginVersionRange {

        private static final String DEFAULT_UNTIL;

        static {
            try {
                Method method = com.gradle.scan.eventmodel.PluginVersion.class.getMethod("until", (Class[]) null);
                DEFAULT_UNTIL = (String) method.getDefaultValue();
            } catch (NoSuchMethodException e) {
                throw new IllegalStateException(e);
            }
        }

        private final PluginVersion since;
        private final PluginVersion until;

        private PluginVersionRange(PluginVersion since, PluginVersion until) {
            this.since = Preconditions.checkNotNull(since);
            this.until = until;
        }

        private static PluginVersionRange from(Class<? extends EventData> eventDataType) {
            com.gradle.scan.eventmodel.PluginVersion[] annotations = eventDataType.getAnnotationsByType(com.gradle.scan.eventmodel.PluginVersion.class);
            assert annotations.length == 1;

            PluginVersion since = parse(annotations[0].since());
            assert since.isValid();

            PluginVersion until = null;
            String untilString = annotations[0].until();
            if (!untilString.equals(DEFAULT_UNTIL)) {
                until = parse(untilString);
                assert until.isValid();
            }

            return new PluginVersionRange(since, until);
        }

        private boolean isInRange(PluginVersion candidate) {
            return candidate.isAtLeast(since)
                && (until == null || candidate.majorAndMinor().isAtMost(until));
        }

    }

    private PluginVersions() {
    }

}
