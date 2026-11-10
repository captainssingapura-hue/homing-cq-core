package com.homing_labs.cq.bench;

import com.homing_labs.cq.scan.ScanResult;
import hue.captains.singapura.tao.ontology.Mutable;
import javafx.collections.ListChangeListener;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.util.List;

/**
 * The dial on the bench's windows, as it stands: measuring until a scan has found something, then reading what it
 * found, and measuring again while the bench scans again. A window with an icon of its own keeps it.
 */
public final class BenchMeter implements Mutable {

    /** The dial as it stands. */
    private List<Image> dial = BenchIcon.INSTANCE.measuring();

    /**
     * Gives the dial to every window shown from now on that has no icon, so the rule book, the taxonomy and any dialog
     * have it too. It is called on the JavaFX thread, before the first window is shown.
     */
    public void onEveryWindow() {
        Window.getWindows().addListener((ListChangeListener<Window>) change -> {
            while (change.next()) {
                change.getAddedSubList().forEach(window -> {
                    if (window instanceof Stage stage && stage.getIcons().isEmpty()) {
                        stage.getIcons().setAll(dial);
                    }
                });
            }
        });
    }

    /** A scan has started: the dial shows no reading. */
    public void measuring() {
        show(BenchIcon.INSTANCE.measuring());
    }

    /** A scan has ended: the dial reads what it found. */
    public void measured(final ScanResult result) {
        show(BenchIcon.INSTANCE.of(result));
    }

    private void show(final List<Image> shown) {
        final List<Image> before = dial;
        dial = shown;
        Window.getWindows().forEach(window -> {
            if (window instanceof Stage stage && stage.getIcons().equals(before)) {
                stage.getIcons().setAll(shown);
            }
        });
    }
}
