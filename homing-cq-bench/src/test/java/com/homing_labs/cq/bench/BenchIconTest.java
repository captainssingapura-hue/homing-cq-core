package com.homing_labs.cq.bench;

import com.homing_labs.cq.scan.Compliance;
import com.homing_labs.cq.scan.ScanResult;
import com.homing_labs.cq.scan.ScannedUnits;
import hue.captains.singapura.tao.ontology.enforcer.OntologyEnforcer;
import javafx.scene.image.Image;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BenchIconTest {

    @BeforeAll
    void startJavaFx() {
        FxThread.INSTANCE.start();
    }

    @Test
    void theNeedleIsInTheGreenOnlyWhenAllComplyAndInTheRedBelowWhereTheBadgesAlarm() {
        Map.ofEntries(Map.entry(0, 0), Map.entry(34, 0), Map.entry(35, 1), Map.entry(69, 1), Map.entry(70, 2),
                        Map.entry(79, 2), Map.entry(80, 3), Map.entry(89, 3), Map.entry(90, 4), Map.entry(99, 4),
                        Map.entry(100, 5))
                .forEach((compliant, reading) -> assertEquals("icon-reading-" + reading + "-32.png",
                        file(BenchIcon.INSTANCE.of(new Compliance(compliant, 100, 0))), compliant + " of 100"));
    }

    @Test
    void aScanThatCheckedNothingGivesNoReading() {
        assertEquals("icon-measuring-32.png", file(BenchIcon.INSTANCE.measuring()));
        assertEquals("icon-measuring-32.png",
                file(BenchIcon.INSTANCE.of(new ScanResult(new ScannedUnits(List.of()), 0))));
        assertEquals("icon-measuring-32.png",
                file(BenchIcon.INSTANCE.of(new ScanResult(new ScannedUnits(List.of()), 3))));
    }

    @Test
    void everyDialLoadsAtEachSizeItIsDrawn() {
        final List<List<Image>> dials = new ArrayList<>(List.of(BenchIcon.INSTANCE.measuring()));
        List.of(0, 40, 75, 85, 95, 100).forEach(compliant ->
                dials.add(BenchIcon.INSTANCE.of(new Compliance(compliant, 100, 0))));
        assertEquals(7, dials.stream().map(this::file).distinct().count());
        dials.forEach(dial -> {
            assertEquals(List.of(32.0, 128.0), dial.stream().map(Image::getWidth).toList());
            assertEquals(List.of(32.0, 128.0), dial.stream().map(Image::getHeight).toList());
            dial.forEach(image -> assertFalse(image.isError(), image.getUrl()));
        });
    }

    @Test
    void itSaysWhatItIsAndIsHeldToIt() {
        assertEquals(List.of(), new OntologyEnforcer().enforce(BenchIcon.class));
    }

    private String file(final List<Image> dial) {
        final String url = dial.getFirst().getUrl();
        return url.substring(url.lastIndexOf('/') + 1);
    }
}
