package com.gradle.export.client.event;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.gradle.scan.eventmodel.EventData;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class VersionedEventType {

    private static final String EVENT_PACKAGE = "com.gradle.scan.eventmodel.";

    private static final LoadingCache<VersionedEventType, VersionedEventType> INTERNED_VERSIONED_EVENT_TYPE = Caffeine.newBuilder().maximumSize(1000).build(k -> k);

    private static final Pattern TYPE_WITH_VERSION_PATTERN = Pattern.compile("([a-zA-Z]+)_(\\d+)_(\\d+)");

    public final String base;
    public final short majorVersion;
    public final short minorVersion;

    private Class<? extends EventData> clazz;

    private VersionedEventType(String base, short majorVersion, short minorVersion) {
        this.base = base;
        this.majorVersion = majorVersion;
        this.minorVersion = minorVersion;
    }

    public static VersionedEventType of(String type, short majorVersion, short minorVersion) {
        return INTERNED_VERSIONED_EVENT_TYPE.get(new VersionedEventType(type, majorVersion, minorVersion));
    }

    public static VersionedEventType of(String type, int majorVersion, int minorVersion) {
        return of(type, (short) majorVersion, (short) minorVersion);
    }

    public static VersionedEventType of(Class<? extends EventData> clazz) throws IllegalArgumentException {
        String name = clazz.getSimpleName();
        Matcher m = TYPE_WITH_VERSION_PATTERN.matcher(name);
        if (m.matches()) {
            String eventTypeName = m.group(1);
            short majorVersion = Short.parseShort(m.group(2));
            short minorVersion = Short.parseShort(m.group(3));
            return of(eventTypeName, majorVersion, minorVersion);
        } else {
            throw new IllegalArgumentException("Event name [" + name + "] not in expected format");
        }
    }

    public String getBase() {
        return base;
    }

    public Class<? extends EventData> getClazz() {
        if (clazz == null) {
            clazz = loadClass();
        }
        return clazz;
    }

    @SuppressWarnings("unchecked")
    private Class<? extends EventData> loadClass() {
        try {
            String className = EVENT_PACKAGE + getFullName();
            return (Class<? extends EventData>) getClass().getClassLoader().loadClass(className);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public String getFullName() {
        return base + "_" + majorVersion + "_" + minorVersion;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        VersionedEventType that = (VersionedEventType) o;

        return base == that.base
            && minorVersion == that.minorVersion
            && majorVersion == that.majorVersion;
    }

    @Override
    public int hashCode() {
        int result = base.hashCode();
        result = 31 * result + (int) majorVersion;
        result = 31 * result + (int) minorVersion;
        return result;
    }

    @Override
    public String toString() {
        return "VersionedEventType{" + getFullName() + '}';
    }

}
