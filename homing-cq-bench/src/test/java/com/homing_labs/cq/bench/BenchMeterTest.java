package com.homing_labs.cq.bench;

import com.homing_labs.cq.core.def.Rule;
import com.homing_labs.cq.core.def.lang.JavaLang;
import com.homing_labs.cq.enforcer.def.Enforcer;
import com.homing_labs.cq.enforcer.def.Result;
import com.homing_labs.cq.lang.java.unit.JavaUnit;
import com.homing_labs.cq.scan.PackageRuleset;
import com.homing_labs.cq.scan.Ruleset;
import com.homing_labs.cq.scan.ScanResult;
import com.homing_labs.cq.scan.Scanner;
import com.homing_labs.cq.scan.rules.Neatness;
import hue.captains.singapura.tao.ontology.enforcer.OntologyEnforcer;
import javafx.application.Platform;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TreeView;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The dial on the bench's windows through a scan. One meter serves every test, as it listens for every window shown for
 * as long as JavaFX runs.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BenchMeterTest {

    private static final String TAXONOMY = "com.homing_labs.cq.lang.java.taxonomy";

    private static final String MEASURING = "icon-measuring-32.png";

    /** Every test rule finds something on every unit it checks, and one of them checks every unit. */
    private static final String NONE_COMPLY = "icon-reading-0-32.png";

    private final ClassLoader loader = getClass().getClassLoader();

    private final List<Ruleset> seen = List.of(new PackageRuleset("com.homing_labs.cq.scan.rules"));

    private final List<Stage> shown = new ArrayList<>();

    private BenchMeter meter;

    @BeforeAll
    void startJavaFx() {
        FxThread.INSTANCE.start();
        meter = FxThread.INSTANCE.call(() -> {
            final BenchMeter made = new BenchMeter();
            made.onEveryWindow();
            return made;
        });
    }

    @BeforeEach
    void measureAfresh() {
        FxThread.INSTANCE.call(() -> {
            meter.measuring();
            return true;
        });
    }

    @AfterEach
    void closeWindows() {
        FxThread.INSTANCE.call(() -> {
            shown.forEach(Stage::close);
            shown.clear();
            return true;
        });
    }

    @Test
    void aWindowShownGetsTheDialAsItStands() {
        assertEquals(MEASURING, FxThread.INSTANCE.call(() -> dial(window())));
        final ScanResult result = Scanner.INSTANCE.scan(List.of(TAXONOMY), seen, List.of(), loader);
        assertEquals(NONE_COMPLY, FxThread.INSTANCE.call(() -> {
            meter.measured(result);
            return dial(window());
        }));
    }

    @Test
    void eachScanMovesTheDialOnEveryWindowThatShowsIt() {
        final ScanResult result = Scanner.INSTANCE.scan(List.of(TAXONOMY), seen, List.of(), loader);
        assertEquals(List.of(NONE_COMPLY, NONE_COMPLY, MEASURING, MEASURING), FxThread.INSTANCE.call(() -> {
            final Stage first = window();
            final Stage second = window();
            meter.measured(result);
            final List<String> dials = new ArrayList<>(List.of(dial(first), dial(second)));
            meter.measuring();
            dials.addAll(List.of(dial(first), dial(second)));
            return dials;
        }));
    }

    @Test
    void aWindowWithAnIconOfItsOwnKeepsIt() {
        final ScanResult result = Scanner.INSTANCE.scan(List.of(TAXONOMY), seen, List.of(), loader);
        assertEquals("icon-reading-5-128.png", FxThread.INSTANCE.call(() -> {
            final Stage own = new Stage();
            own.getIcons().setAll(new Image(BenchIcon.class.getResource("icon-reading-5-128.png").toExternalForm()));
            show(own);
            meter.measured(result);
            return dial(own);
        }));
    }

    @Test
    void theBenchOpensMeasuringAndReadsWhenItsScanHasFound() {
        final Held held = new Held();
        final Stage bench = FxThread.INSTANCE.call(() -> bench(held, seen));
        assertEquals(List.of(MEASURING, "true", "true", "true"), FxThread.INSTANCE.call(() -> List.of(dial(bench),
                String.valueOf(status(bench).startsWith("Scanning " + TAXONOMY)),
                String.valueOf(scan(bench).isDisabled()),
                String.valueOf(units(bench).getRoot() == null))));
        FxThread.INSTANCE.call(held::release);
        assertEquals(NONE_COMPLY, FxThread.INSTANCE.call(() -> dial(bench)));
        assertFalse(FxThread.INSTANCE.call(() -> scan(bench).isDisabled()));
        assertNotNull(FxThread.INSTANCE.call(() -> units(bench).getRoot()));
        assertTrue(FxThread.INSTANCE.call(() -> status(bench)).contains(" comply with "));
    }

    @Test
    void scanningAgainMeasuresAgainAndOneScanRunsAtATime() {
        final Held held = new Held();
        final Stage bench = FxThread.INSTANCE.call(() -> bench(held, seen));
        FxThread.INSTANCE.call(held::release);
        assertEquals(1, FxThread.INSTANCE.call(() -> {
            scan(bench).fire();
            scan(bench).fire();
            return held.waiting();
        }), "pressing Scan again while it scans does nothing");
        assertEquals(MEASURING, FxThread.INSTANCE.call(() -> dial(bench)));
        assertNotNull(FxThread.INSTANCE.call(() -> units(bench).getRoot()), "what it found last stays in view");
        FxThread.INSTANCE.call(held::release);
        assertEquals(NONE_COMPLY, FxThread.INSTANCE.call(() -> dial(bench)));
    }

    @Test
    void aClassThatCannotBeLinkedEndsTheScanSayingSoAndLeavesNoReading() {
        final Stage bench = FxThread.INSTANCE.call(() -> bench(ScanThread.AtOnce.INSTANCE, List.of(new Unlinkable())));
        assertEquals(MEASURING, FxThread.INSTANCE.call(() -> dial(bench)));
        assertFalse(FxThread.INSTANCE.call(() -> scan(bench).isDisabled()));
        assertNull(FxThread.INSTANCE.call(() -> units(bench).getRoot()));
        assertEquals("Cannot scan " + TAXONOMY + " by unlinkable: a.missing.Type",
                FxThread.INSTANCE.call(() -> status(bench)));
    }

    @Test
    void inTheBackgroundAScanRunsOffTheJavaFxThreadAndHandsBackOnIt() throws Exception {
        final CompletableFuture<List<Boolean>> threads = new CompletableFuture<>();
        ScanThread.InBackground.INSTANCE.run(Platform::isFxApplicationThread,
                onIt -> threads.complete(List.of(onIt, Platform.isFxApplicationThread())));
        assertEquals(List.of(false, true), threads.get(10, TimeUnit.SECONDS));
    }

    @Test
    void itsTypesSayWhatTheyAreAndAreHeldToIt() {
        for (final Class<?> type : List.of(BenchMeter.class, ScanThread.AtOnce.class, ScanThread.InBackground.class)) {
            assertEquals(List.of(), new OntologyEnforcer().enforce(type), type.getName());
        }
    }

    private Stage window() {
        final Stage stage = new Stage();
        show(stage);
        return stage;
    }

    private void show(final Stage stage) {
        stage.setScene(new Scene(new Group(), 100, 100));
        stage.show();
        shown.add(stage);
    }

    private Stage bench(final ScanThread thread, final List<Ruleset> rulesets) {
        final Stage stage = new Stage();
        stage.setScene(new Scene(BenchPane.INSTANCE.content(TAXONOMY, rulesets, List.of(), loader, List.of(),
                new SourceView(), thread, meter), 900, 600));
        stage.show();
        shown.add(stage);
        return stage;
    }

    private String dial(final Stage stage) {
        final String url = stage.getIcons().getFirst().getUrl();
        return url.substring(url.lastIndexOf('/') + 1);
    }

    private String status(final Stage bench) {
        return ((Label) bench.getScene().getRoot().lookup("#status")).getText();
    }

    private Button scan(final Stage bench) {
        return (Button) bench.getScene().getRoot().lookup("#scan");
    }

    @SuppressWarnings("unchecked")
    private TreeView<IndexEntry> units(final Stage bench) {
        return (TreeView<IndexEntry>) bench.getScene().getRoot().lookup("#units");
    }

    /** Holds each scan back until the test lets it go. */
    private static final class Held implements ScanThread {

        private final List<Runnable> held = new ArrayList<>();

        @Override
        public <T> void run(final Supplier<T> scan, final Consumer<T> done) {
            held.add(() -> done.accept(scan.get()));
        }

        int waiting() {
            return held.size();
        }

        boolean release() {
            held.removeFirst().run();
            return true;
        }
    }

    /** A ruleset whose enforcer needs a class that cannot be found, as code with a missing dependency does. */
    private record Unlinkable() implements Ruleset {

        @Override
        public String name() {
            return "unlinkable";
        }

        @Override
        public List<Enforcer<?, ?, ?>> enforcers(final ClassLoader loader) {
            return List.of(new MissingEnforcer());
        }
    }

    record Missing() implements Rule<Neatness, JavaLang> {
    }

    record MissingEnforcer() implements Enforcer<JavaLang, Missing, JavaUnit> {

        @Override
        public Result<JavaLang, Missing> check(final JavaUnit unit) {
            throw new NoClassDefFoundError("a.missing.Type");
        }
    }
}
