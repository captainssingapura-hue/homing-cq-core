package com.homing_labs.cq.bench;

import com.homing_labs.cq.lang.java.model.CodeBlocks;
import com.homing_labs.cq.lang.java.outline.CodeBlock;
import hue.captains.singapura.tao.ontology.Mutable;
import hue.captains.singapura.tao.ontology.Scalar;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.text.Font;
import javafx.scene.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

/**
 * A source file shown line by line, each under its number, in a fixed-width font, with stretches of it highlighted:
 * those something is written in, and, lighter, those it is written somewhere within. Only the lines in view are laid
 * out, so a file of any length shows at once. The lines are scrolled through, not selected.
 */
public final class SourceLines implements Mutable {

    private static final String WRITTEN = "#ffe08a";

    private static final String WITHIN = "#dbe8fb";

    private static final String NUMBER = "#9a9a9a";

    private final ListView<Line> list = new ListView<>();

    private final Font font = Font.font("Monospaced", Font.getDefault().getSize());

    private String text = "";

    private List<CodeBlock> written = List.of();

    private List<CodeBlock> within = List.of();

    /** How wide the line numbers are: as wide as the last line's, and the space after it. */
    private double numberWidth;

    public SourceLines() {
        list.setId("source-lines");
        list.setCellFactory(view -> new LineCell());
        list.setFixedCellSize(Math.ceil(lineHeight()) + 2);
        list.setFocusTraversable(false);
        list.setStyle("-fx-selection-bar: transparent; -fx-selection-bar-non-focused: transparent;");
    }

    /** What it shows. */
    public Node content() {
        return list;
    }

    /** Shows the text from its first line, with nothing highlighted. */
    public void show(final String text) {
        this.text = text;
        written = List.of();
        within = List.of();
        final List<Line> lines = lines(text);
        numberWidth = width("0".repeat(String.valueOf(lines.size()).length())) + 14;
        list.getItems().setAll(lines);
        list.scrollTo(0);
    }

    /** Highlights the stretches of the text shown, and brings the first of them into view. */
    public void highlight(final CodeBlocks written, final CodeBlocks within) {
        this.written = written.items();
        this.within = within.items();
        list.refresh();
        final List<CodeBlock> first = !this.written.isEmpty() ? this.written : this.within;
        if (!first.isEmpty()) {
            list.scrollTo(Math.max(0, lineOf(first.getFirst().offset()) - 2));
        }
    }

    /** The text shown. */
    public String text() {
        return text;
    }

    /** The stretches highlighted as where something is written, as they read. */
    public List<String> written() {
        return written.stream().map(CodeBlock::text).toList();
    }

    /** The stretches highlighted, lighter, as where something is written somewhere within, as they read. */
    public List<String> within() {
        return within.stream().map(CodeBlock::text).toList();
    }

    /** The lines of a text, each without its line terminator: {@code \n}, {@code \r\n} or {@code \r}. */
    private List<Line> lines(final String text) {
        final List<Line> lines = new ArrayList<>();
        int start = 0;
        for (int at = 0; at < text.length(); at++) {
            final char c = text.charAt(at);
            if (c == '\n' || c == '\r') {
                lines.add(new Line(lines.size() + 1, start, at));
                if (c == '\r' && at + 1 < text.length() && text.charAt(at + 1) == '\n') {
                    at++;
                }
                start = at + 1;
            }
        }
        if (start < text.length() || lines.isEmpty()) {
            lines.add(new Line(lines.size() + 1, start, text.length()));
        }
        return lines;
    }

    /** The index of the line a character is on. */
    private int lineOf(final int character) {
        final List<Line> lines = list.getItems();
        int low = 0;
        int high = lines.size() - 1;
        while (low < high) {
            final int middle = (low + high + 1) / 2;
            if (lines.get(middle).start() <= character) {
                low = middle;
            } else {
                high = middle - 1;
            }
        }
        return low;
    }

    private double lineHeight() {
        return probe("Mg|").getHeight();
    }

    private double width(final String text) {
        return probe(text).getWidth();
    }

    private Bounds probe(final String text) {
        final Text probe = new Text(text);
        probe.setFont(font);
        return probe.getLayoutBounds();
    }

    /** How a character is shown: plain, or highlighted as written there, or within there. */
    private String backgroundAt(final int character) {
        if (covers(written, character)) {
            return WRITTEN;
        }
        return covers(within, character) ? WITHIN : "transparent";
    }

    private boolean covers(final List<CodeBlock> spans, final int character) {
        return spans.stream().anyMatch(span -> span.offset() <= character && character < end(span));
    }

    /** Where a block ends in the file: past its last character. */
    private int end(final CodeBlock block) {
        return block.offset() + block.text().length();
    }

    private boolean touches(final List<CodeBlock> spans, final Line line) {
        return spans.stream().anyMatch(span -> span.offset() < Math.max(line.end(), line.start() + 1)
                && line.start() < end(span));
    }

    /**
     * A line of the text: its number, from 1, and where it is, from its first character up to, not including, its
     * terminator.
     */
    public record Line(int number, int start, int end) implements Scalar {
    }

    /**
     * A line, as its number and its text, in stretches each highlighted as it is. A line whose terminator is
     * highlighted too is highlighted to the edge, so that a block reads as one.
     */
    private final class LineCell extends ListCell<Line> {

        @Override
        protected void updateItem(final Line line, final boolean empty) {
            super.updateItem(line, empty);
            setText(null);
            final String across = empty || line == null || line.end() >= text.length() ? "transparent"
                    : backgroundAt(line.end());
            setStyle("-fx-padding: 0 8 0 0; -fx-background-color: " + across + ";");
            if (empty || line == null) {
                setGraphic(null);
                return;
            }
            final HBox row = new HBox(number(line));
            row.setAlignment(Pos.CENTER_LEFT);
            final TreeSet<Integer> cuts = new TreeSet<>(List.of(line.start(), line.end()));
            for (final CodeBlock span : spans()) {
                if (span.offset() > line.start() && span.offset() < line.end()) {
                    cuts.add(span.offset());
                }
                if (end(span) > line.start() && end(span) < line.end()) {
                    cuts.add(end(span));
                }
            }
            Integer from = cuts.first();
            for (Integer to = cuts.higher(from); to != null; from = to, to = cuts.higher(to)) {
                row.getChildren().add(stretch(text.substring(from, to), backgroundAt(from)));
            }
            setGraphic(row);
        }

        private List<CodeBlock> spans() {
            final List<CodeBlock> all = new ArrayList<>(written);
            all.addAll(within);
            return all;
        }

        /** The line's number, marked when something highlighted is on the line. */
        private Label number(final Line line) {
            final Label number = new Label(String.valueOf(line.number()));
            number.setFont(font);
            number.setMinWidth(Region.USE_PREF_SIZE);
            number.setPrefWidth(numberWidth);
            number.setAlignment(Pos.CENTER_RIGHT);
            number.setPadding(new Insets(0, 10, 0, 0));
            final String mark = touches(written, line) ? WRITTEN : touches(within, line) ? WITHIN : "transparent";
            number.setStyle("-fx-text-fill: " + NUMBER + "; -fx-background-color: -fx-control-inner-background;"
                    + " -fx-border-color: transparent " + mark + " transparent transparent;"
                    + " -fx-border-width: 0 3 0 0;");
            return number;
        }

        private Label stretch(final String part, final String background) {
            final Label stretch = new Label(part.replace("\t", "    "));
            stretch.setFont(font);
            stretch.setMinWidth(Region.USE_PREF_SIZE);
            stretch.setPadding(Insets.EMPTY);
            stretch.setStyle("-fx-text-fill: #1e1e1e; -fx-background-color: " + background + ";");
            return stretch;
        }
    }
}
