package com.homing_labs.cq.rules.basic.enforce;

import com.homing_labs.cq.lang.java.outline.CodeBlock;
import hue.captains.singapura.tao.ontology.Vector;

import java.util.List;

/**
 * A source file's lines, from its first, each in the effective lines {@link EffectiveLines} counts it as: what the size
 * rules sum over a stretch of the file, and a rule of other numbers can too.
 */
public record LineCounts(List<LineCount> items) implements Vector<LineCount> {

    public LineCounts {
        items = List.copyOf(items);
    }

    /** The effective lines a block of the file is written in: those of every line it is on, whole. */
    public int of(final CodeBlock block) {
        final int first = block.line() - 1;
        int lines = 1;
        final String text = block.text();
        for (int at = 0; at < text.length(); at++) {
            final char c = text.charAt(at);
            if (c == '\n' || c == '\r' && (at + 1 == text.length() || text.charAt(at + 1) != '\n')) {
                lines++;
            }
        }
        return items.subList(first, Math.min(first + lines, items.size())).stream()
                .mapToInt(LineCount::effectiveLines).sum();
    }
}
