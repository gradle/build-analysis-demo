package com.gradle.export.client.util;

import ratpack.exec.Operation;
import ratpack.exec.internal.DefaultExecController;
import ratpack.func.Block;
import ratpack.util.Exceptions;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public final class StandaloneRatpackHarness {

    public static void execute(Operation op) {
        execute(op, false);
    }

    public static void execute(Operation op, boolean awaitBackgroundTermination) {
        AtomicReference<Throwable> error = new AtomicReference<>();
        DefaultExecController controller = new DefaultExecController();
        try {
            CountDownLatch done = new CountDownLatch(1);
            controller.fork()
                .onComplete(e -> done.countDown())
                .onError(error::set)
                .start(op);
            Exceptions.uncheck((Block) done::await);
        } finally {
            controller.close();
            if (awaitBackgroundTermination) {
                Exceptions.uncheck(() -> controller.getExecutor().awaitTermination(10, TimeUnit.MINUTES));
            }
        }
        if (error.get() != null) {
            throw Exceptions.uncheck(error.get());
        }
    }

    private StandaloneRatpackHarness() {
    }

}
