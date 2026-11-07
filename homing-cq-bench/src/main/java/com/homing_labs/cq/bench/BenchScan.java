package com.homing_labs.cq.bench;

import com.homing_labs.cq.scan.Exemptions;
import com.homing_labs.cq.scan.Ruleset;
import com.homing_labs.cq.scan.ScanResult;
import com.homing_labs.cq.scan.ScannedUnit;
import com.homing_labs.cq.scan.ScannedUnits;
import com.homing_labs.cq.scan.Scanner;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TreeView;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * The bench's scan: asks the {@link ScanThread} to run the {@link Scanner}, and shows what it found. One scan runs at a
 * time, and until it ends the bench says so, the dial measures, and what the last scan found stays in view.
 *
 * @param listener told what each scan came to
 * @param cleared  run after each scan, for what was selected in what the last one found
 */
record BenchScan(List<Ruleset> rulesets, List<Path> sources, List<Exemptions<?>> exemptions, ClassLoader loader,
                 ScanThread thread, BenchMeter meter, TreeView<IndexEntry> units, Label status, Button button,
                 Consumer<ScanResult> listener, Runnable cleared) {

    /** Scans the root packages, one package or several apart by commas or spaces, unless a scan is still running. */
    void run(final String rootPackages) {
        if (button.isDisabled()) {
            return;
        }
        final List<String> roots = Arrays.stream(rootPackages.split("[,\\s]+")).filter(root -> !root.isEmpty())
                .distinct().toList();
        button.setDisable(true);
        meter.measuring();
        status.setText("Scanning " + String.join(", ", roots) + " by " + names() + "...");
        thread.run(() -> scanned(roots), outcome -> {
            button.setDisable(false);
            outcome.failure().ifPresentOrElse(why -> failed(roots, why), () -> found(outcome.result(), roots));
            cleared.run();
        });
    }

    /** The rulesets by their names: {@code no ruleset}, when there is none. */
    String names() {
        return rulesets.isEmpty() ? "no ruleset" : rulesets.stream().map(Ruleset::name)
                .collect(Collectors.joining(", "));
    }

    /**
     * The scan itself, on the scan's thread. A class that cannot be linked is said like any other failure: on a
     * thread of its own, it would end the scan with nothing shown and the dial measuring for good.
     */
    private Outcome scanned(final List<String> roots) {
        try {
            return new Outcome(Scanner.INSTANCE.scan(roots, rulesets, sources, exemptions, loader), Optional.empty());
        } catch (final RuntimeException | LinkageError e) {
            return new Outcome(nothing(), Optional.of(String.valueOf(e.getMessage())));
        }
    }

    /** Shows what a scan found; or, if it cannot, says why and shows none. */
    private void found(final ScanResult result, final List<String> roots) {
        try {
            listener.accept(result);
            meter.measured(result);
            final List<ScannedUnit> found = result.units().items();
            units.setRoot(UnitTree.INSTANCE.of(found));
            status.setText(count(found.size(), "unit") + " in "
                    + count((int) found.stream().map(scanned -> scanned.unit().type().getPackageName()).distinct()
                    .count(), "package") + " under " + String.join(", ", roots) + "; "
                    + result.compliance().compliant() + " comply with the " + count(result.rules(), "rule") + " of "
                    + names() + exempted(result));
        } catch (final RuntimeException e) {
            failed(roots, String.valueOf(e.getMessage()));
        }
    }

    /** How the exemptions stand, as they do not move the dial: nothing, when there are none. */
    private String exempted(final ScanResult result) {
        return result.exemptions().items().isEmpty() ? ""
                : "; " + count(result.exemptionsUsed(), "exemption") + " used, " + result.stale().size() + " stale";
    }

    private void failed(final List<String> roots, final String why) {
        listener.accept(nothing());
        meter.measured(nothing());
        units.setRoot(null);
        status.setText("Cannot scan " + String.join(", ", roots) + " by " + names() + ": " + why);
    }

    private ScanResult nothing() {
        return new ScanResult(new ScannedUnits(List.of()), 0);
    }

    private String count(final int count, final String thing) {
        return count + " " + thing + (count == 1 ? "" : "s");
    }

    /** What a scan came to: what it found, and why it could not, if it could not. */
    private record Outcome(ScanResult result, Optional<String> failure) {
    }
}
