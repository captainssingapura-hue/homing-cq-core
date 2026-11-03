package com.homing_labs.cq.bench;

import hue.captains.singapura.tao.ontology.StatelessFunctionalObject;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * A documentation comment as plain text, as the rule book shows it: its description alone - the block tags after it,
 * {@code @param} and the like, are left out - its inline tags as the words they show, the classes it links to by
 * their simple names, its HTML as paragraphs, and each paragraph on one line.
 */
public record DocText() implements StatelessFunctionalObject {

    public static final DocText INSTANCE = new DocText();

    /** The comment, as written from its {@code /**} to its end, as plain text: its paragraphs apart by a blank line. */
    public String of(final String comment) {
        final String description = description(comment);
        final StringBuilder text = new StringBuilder();
        int at = 0;
        int tag = description.indexOf("{@");
        while (tag >= 0) {
            text.append(html(description.substring(at, tag)));
            final int close = closing(description, tag);
            text.append(inline(description.substring(tag + 2, close)));
            at = Math.min(close + 1, description.length());
            tag = description.indexOf("{@", at);
        }
        text.append(html(description.substring(at)));
        return Arrays.stream(text.toString().split("\\n\\s*\\n"))
                .map(paragraph -> paragraph.strip().replaceAll("\\s+", " "))
                .filter(paragraph -> !paragraph.isEmpty())
                .collect(Collectors.joining("\n\n"));
    }

    /** The comment's description: its lines without the comment's marks, up to its first block tag. */
    private String description(final String comment) {
        return comment.replaceFirst("^/\\*\\*", "").replaceFirst("\\*/$", "").lines()
                .map(line -> line.replaceFirst("^\\s*\\*? ?", ""))
                .takeWhile(line -> !line.startsWith("@"))
                .collect(Collectors.joining("\n"));
    }

    /** Where the inline tag that opens at {@code open} closes, its braces counted: the end, when it does not. */
    private int closing(final String text, final int open) {
        int depth = 0;
        for (int at = open; at < text.length(); at++) {
            if (text.charAt(at) == '{') {
                depth++;
            } else if (text.charAt(at) == '}' && --depth == 0) {
                return at;
            }
        }
        return text.length();
    }

    /** HTML as plain text: a paragraph, a break or a list as a new paragraph, an item of a list as one with a dash. */
    private String html(final String text) {
        return text.replaceAll("(?i)<\\s*/?\\s*(?:p|br|ul|ol|pre|blockquote|h[1-6])\\b[^>]*>", "\n\n")
                .replaceAll("(?i)<\\s*li\\b[^>]*>", "\n\n- ")
                .replaceAll("<[^>]+>", "")
                .replace("&lt;", "<").replace("&gt;", ">").replace("&quot;", "\"").replace("&#39;", "'")
                .replace("&nbsp;", " ").replace("&amp;", "&");
    }

    /** An inline tag, from its name to its closing brace, as the words it shows. */
    private String inline(final String tag) {
        final String name = tag.split("\\s", 2)[0];
        final String rest = tag.substring(name.length()).strip();
        return switch (name) {
            case "link", "linkplain" -> linked(rest);
            case "inheritDoc" -> "";
            default -> rest;
        };
    }

    /**
     * A link as the words it shows: its label, if it has one; else what it refers to, without packages, a member after
     * its class's name and a dot - {@code Foo.bar(int)} - or alone.
     */
    private String linked(final String link) {
        int depth = 0;
        int end = 0;
        while (end < link.length() && (depth > 0 || !Character.isWhitespace(link.charAt(end)))) {
            depth += link.charAt(end) == '(' ? 1 : link.charAt(end) == ')' ? -1 : 0;
            end++;
        }
        final String label = link.substring(end).strip();
        return !label.isEmpty() ? label : link.substring(0, end).replaceFirst("^#", "").replace('#', '.')
                .replaceAll("\\b(?:[a-z_][a-z0-9_]*\\.)+(?=[A-Za-z_])", "");
    }
}
