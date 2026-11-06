package com.homing_labs.cq.bench;

import hue.captains.singapura.tao.ontology.enforcer.OntologyEnforcer;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BenchIconTest {

    @BeforeAll
    void startJavaFx() {
        FxThread.INSTANCE.start();
        FxThread.INSTANCE.call(() -> {
            BenchIcon.INSTANCE.onEveryWindow();
            return true;
        });
    }

    @Test
    void theIconLoadsAtEachSizeItIsDrawn() {
        final List<Image> images = BenchIcon.INSTANCE.images();
        assertEquals(List.of(32.0, 128.0), images.stream().map(Image::getWidth).toList());
        assertEquals(List.of(32.0, 128.0), images.stream().map(Image::getHeight).toList());
        images.forEach(image -> assertFalse(image.isError()));
    }

    @Test
    void aWindowShownWithNoIconGetsTheBenchs() {
        assertEquals(List.of(32.0, 128.0), FxThread.INSTANCE.call(() -> {
            final Stage stage = new Stage();
            stage.setScene(new Scene(new Group(), 100, 100));
            stage.show();
            final List<Double> sizes = stage.getIcons().stream().map(Image::getWidth).toList();
            stage.close();
            return sizes;
        }));
    }

    @Test
    void aWindowWithAnIconOfItsOwnKeepsIt() {
        assertEquals(List.of(128.0), FxThread.INSTANCE.call(() -> {
            final Stage stage = new Stage();
            stage.getIcons().add(BenchIcon.INSTANCE.images().get(1));
            stage.setScene(new Scene(new Group(), 100, 100));
            stage.show();
            final List<Double> sizes = stage.getIcons().stream().map(Image::getWidth).toList();
            stage.close();
            return sizes;
        }));
    }

    @Test
    void itSaysWhatItIsAndIsHeldToIt() {
        assertEquals(List.of(), new OntologyEnforcer().enforce(BenchIcon.class));
    }
}
