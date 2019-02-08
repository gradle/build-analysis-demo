package com.gradle.export.client.util;

import java.util.Optional;
import java.util.function.Function;

public final class Ref<T> {

    private T value;

    public static <T> Ref<T> empty() {
        return of(null);
    }

    public static <T> Ref<T> of(T value) {
        return new Ref<>(value);
    }

    private Ref(T value) {
        this.value = value;
    }

    public void set(T value) {
        this.value = value;
    }

    public T getAndSet(T value) {
        T oldValue = this.value;
        this.value = value;
        return oldValue;
    }

    public void update(Function<? super T, ? extends T> function) {
        this.value = function.apply(value);
    }

    public T updateAndGet(Function<? super T, ? extends T> function) {
        this.value = function.apply(value);
        return value;
    }

    public Optional<T> maybeGet() {
        return Optional.ofNullable(value);
    }

    public T get() {
        if (value == null) {
            throw new IllegalStateException("value is null");
        }

        return value;
    }

    public boolean isEmpty() {
        return value == null;
    }

    @Override
    public String toString() {
        return "Ref{value=" + value + '}';
    }
}
