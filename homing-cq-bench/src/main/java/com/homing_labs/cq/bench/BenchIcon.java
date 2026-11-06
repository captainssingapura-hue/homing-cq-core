package com.homing_labs.cq.bench;

import hue.captains.singapura.tao.ontology.StatelessFunctionalObject;
import javafx.collections.ListChangeListener;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.util.List;
import java.util.stream.Stream;

/**
 * The bench's icon, a dial with its needle in the green, for its windows' title bars and the taskbar. It comes from
 * image files: an image made in memory is taken without complaint, but Windows still shows the Java cup.
 */
public record BenchIcon() implements StatelessFunctionalObject {

    public static final BenchIcon INSTANCE = new BenchIcon();

    /** The icon at each size it is drawn: JavaFX gives Windows the 32 pixel one, and other platforms the 128. */
    public List<Image> images() {
        return Stream.of("icon-32.png", "icon-128.png").map(this::image).toList();
    }

    /**
     * Gives the icon to every window shown from now on that has none of its own, so the rule book, the taxonomy and any
     * dialog have it too. It is called on the JavaFX thread, before the first window is shown.
     */
    public void onEveryWindow() {
        final List<Image> images = images();
        Window.getWindows().addListener((ListChangeListener<Window>) change -> {
            while (change.next()) {
                change.getAddedSubList().forEach(window -> {
                    if (window instanceof Stage stage && stage.getIcons().isEmpty()) {
                        stage.getIcons().addAll(images);
                    }
                });
            }
        });
    }

    private Image image(final String file) {
        return new Image(getClass().getResource(file).toExternalForm());
    }
}
