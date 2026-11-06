package com.homing_labs.cq.bench;

import com.homing_labs.cq.scan.Compliance;
import com.homing_labs.cq.scan.ScanResult;
import hue.captains.singapura.tao.ontology.StatelessFunctionalObject;
import javafx.scene.image.Image;

import java.util.List;
import java.util.stream.Stream;

/**
 * The bench's icon, for its windows' title bars and the taskbar: a dial, measuring while the bench scans, and then
 * reading the share of units that comply. It agrees with the {@link Badges}: the needle is in the green only when
 * all of them do, in the amber down to {@link Badges#ALARM_BELOW}, and in the red below that. It comes from image
 * files, one for each reading: an image made in memory is taken without complaint, but Windows still shows the Java
 * cup.
 */
public record BenchIcon() implements StatelessFunctionalObject {

    public static final BenchIcon INSTANCE = new BenchIcon();

    /** The reading when every unit complies. */
    private static final int ALL_COMPLY = 5;

    /** The dial with no reading: while measuring, and when a scan checked nothing. */
    public List<Image> measuring() {
        return images("measuring");
    }

    /** The dial reading what a scan found; a scan that read no unit, or had no rule to check by, gives no reading. */
    public List<Image> of(final ScanResult result) {
        return result.units().items().isEmpty() || result.rules() == 0 ? measuring() : of(result.compliance());
    }

    /** The dial reading how far some units comply. */
    public List<Image> of(final Compliance compliance) {
        return images("reading-" + reading(compliance));
    }

    /** Where the needle points, from 0 in the red to 5 in the green. */
    private int reading(final Compliance compliance) {
        return compliance.complete() ? ALL_COMPLY
                : (int) Stream.of(Badges.ALARM_BELOW / 2, Badges.ALARM_BELOW, 0.8, 0.9)
                .filter(step -> compliance.share() >= step)
                .count();
    }

    /** A dial at each size it is drawn: JavaFX gives Windows the 32 pixel one, and other platforms the 128. */
    private List<Image> images(final String dial) {
        return Stream.of(32, 128)
                .map(size -> new Image(getClass().getResource("icon-" + dial + "-" + size + ".png").toExternalForm()))
                .toList();
    }
}
