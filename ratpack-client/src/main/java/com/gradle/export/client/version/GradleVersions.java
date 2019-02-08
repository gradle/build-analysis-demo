package com.gradle.export.client.version;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.gradle.scan.eventmodel.EventData;
import ratpack.func.Pair;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import static com.google.common.collect.Lists.transform;
import static com.gradle.export.client.event.EventDataHierarchy.HIERARCHY;

public final class GradleVersions {

    private static final LoadingCache<Pair<Class<? extends EventData>, GradleVersion>, Boolean> MATCH_FOR_EVENT_DATA_CLASS_GRADLE_VERSION_PAIR = Caffeine.newBuilder()
        .maximumSize(10000).build(GradleVersions::isEventDataTypeCaptured);


    private static final LoadingCache<Pair<Set<Class<? extends EventData>>, GradleVersion>, Boolean> MATCH_FOR_EVENT_DATA_CLASSES_GRADLE_VERSION_PAIR = Caffeine.newBuilder()
        .maximumSize(10000).build(GradleVersions::isEventDataTypesCaptured);

    private static final LoadingCache<String, GradleVersion> GRADLE_VERSION_FOR_VERSION_PATTERN = Caffeine.newBuilder()
        .maximumSize(1000).build(GradleVersion::new);


    public static boolean isCaptured(Set<Class<? extends EventData>> eventDataTypes, GradleVersion gradleVersion) {
        return MATCH_FOR_EVENT_DATA_CLASSES_GRADLE_VERSION_PAIR.get(Pair.of(eventDataTypes, gradleVersion));
    }

    private static boolean isEventDataTypesCaptured(Pair<Set<Class<? extends EventData>>, GradleVersion> pair) {
        Set<Class<? extends EventData>> eventDataTypes = pair.left;
        GradleVersion gradleVersion = pair.right;
        return Iterables.all(eventDataTypes, e -> MATCH_FOR_EVENT_DATA_CLASS_GRADLE_VERSION_PAIR.get(Pair.of(e, gradleVersion)));
    }

    private static boolean isEventDataTypeCaptured(Pair<Class<? extends EventData>, GradleVersion> pair) {
        Class<? extends EventData> eventDataType = pair.left;
        GradleVersion gradleVersion = pair.right;
        return isCapturedForGradle(eventDataType, gradleVersion) || isSubClassCapturedForGradle(eventDataType, gradleVersion);
    }

    public static boolean isCapturedForGradle(Class<? extends EventData> eventDataType, GradleVersion gradleVersion) {
        GradleVersionRange range = GradleVersionRange.from(eventDataType);
        return range.isInRange(gradleVersion) && !range.excludes(gradleVersion);
    }

    private static boolean isSubClassCapturedForGradle(Class<? extends EventData> eventDataType, GradleVersion gradleVersion) {
        Collection<Class<? extends EventData>> hierarchy = HIERARCHY.get(eventDataType);
        return hierarchy != null && Iterables.tryFind(hierarchy, child -> isCapturedForGradle(child, gradleVersion)).isPresent();
    }

    public static GradleVersion parse(String version) {
        return GRADLE_VERSION_FOR_VERSION_PATTERN.get(version);
    }

    private static final class GradleVersionRange {

        private static final String DEFAULT_UNTIL;

        static {
            try {
                Method method = com.gradle.scan.eventmodel.GradleVersion.class.getMethod("until", (Class[]) null);
                DEFAULT_UNTIL = (String) method.getDefaultValue();
            } catch (NoSuchMethodException e) {
                throw new IllegalStateException(e);
            }
        }

        private final GradleVersion since;
        private final GradleVersion until;
        private final Set<GradleVersion> except;

        private GradleVersionRange(GradleVersion since, GradleVersion until, Collection<GradleVersion> except) {
            this.since = Preconditions.checkNotNull(since);
            this.until = until;
            this.except = ImmutableSet.copyOf(except);
        }

        private static GradleVersionRange from(Class<? extends EventData> eventDataType) {
            com.gradle.scan.eventmodel.GradleVersion[] annotations = eventDataType.getAnnotationsByType(com.gradle.scan.eventmodel.GradleVersion.class);
            assert annotations.length == 1 : eventDataType.getSimpleName() + " should have GradleVersion";

            GradleVersion since = parse(annotations[0].since());
            assert since.isValid();

            GradleVersion until = null;
            String untilString = annotations[0].until();
            if (!untilString.equals(DEFAULT_UNTIL)) {
                until = parse(untilString);
                assert until.isValid();
            }

            List<GradleVersion> except = transform(ImmutableList.copyOf(annotations[0].except()), GradleVersions::parse);

            return new GradleVersionRange(since, until, except);
        }

        private boolean isInRange(GradleVersion candidate) {
            return candidate.isAtLeast(since)
                && (until == null || candidate.majorAndMinor().isAtMost(until));
        }

        private boolean excludes(GradleVersion candidate) {
            return except.contains(candidate);
        }

    }

    private GradleVersions() {
    }

}
