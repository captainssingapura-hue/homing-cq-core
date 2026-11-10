package com.homing_labs.cq.bench;

import javafx.application.Platform;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

/**
 * The JavaFX thread, for tests: started once, kept running between them, and given work to do and wait for. It needs a
 * desktop to start on, as JavaFX does.
 */
record FxThread() {

    static final FxThread INSTANCE = new FxThread();

    /** Starts JavaFX, unless an earlier test has. */
    void start() {
        try {
            Platform.startup(() -> {
            });
        } catch (final IllegalStateException startedAlready) {
            // an earlier test started it
        }
        Platform.setImplicitExit(false);
    }

    /** Does the work on the JavaFX thread, and waits for what it gives. */
    <T> T call(final Supplier<T> work) {
        final CompletableFuture<T> result = new CompletableFuture<>();
        Platform.runLater(() -> {
            try {
                result.complete(work.get());
            } catch (final Throwable failure) {
                result.completeExceptionally(failure);
            }
        });
        try {
            return result.get(30, TimeUnit.SECONDS);
        } catch (final ExecutionException e) {
            if (e.getCause() instanceof RuntimeException failure) {
                throw failure;
            }
            if (e.getCause() instanceof Error failure) {
                throw failure;
            }
            throw new IllegalStateException(e.getCause());
        } catch (final InterruptedException | TimeoutException e) {
            throw new IllegalStateException("The JavaFX thread did not answer", e);
        }
    }
}
