package com.homing_labs.cq.bench;

import hue.captains.singapura.tao.ontology.FunctionalObject;
import hue.captains.singapura.tao.ontology.StatelessFunctionalObject;
import javafx.application.Platform;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Where the bench's scans run. A scan can take a while, so the bench itself runs them {@link InBackground}: its window
 * opens at once and stays alive. Code that wants what a scan found before it goes on runs them {@link AtOnce}.
 */
public interface ScanThread extends FunctionalObject {

    /** Runs the scan, then hands what it came to to {@code done} on the JavaFX thread. */
    <T> void run(Supplier<T> scan, Consumer<T> done);

    /** On the thread that asks, which is the JavaFX thread, and done by the time it returns. */
    record AtOnce() implements ScanThread, StatelessFunctionalObject {

        public static final AtOnce INSTANCE = new AtOnce();

        @Override
        public <T> void run(final Supplier<T> scan, final Consumer<T> done) {
            done.accept(scan.get());
        }
    }

    /** On a thread of its own, which does not keep the program alive once the bench is closed. */
    record InBackground() implements ScanThread, StatelessFunctionalObject {

        public static final InBackground INSTANCE = new InBackground();

        @Override
        public <T> void run(final Supplier<T> scan, final Consumer<T> done) {
            Thread.ofPlatform().daemon().name("bench-scan").start(() -> {
                final T scanned = scan.get();
                Platform.runLater(() -> done.accept(scanned));
            });
        }
    }
}
