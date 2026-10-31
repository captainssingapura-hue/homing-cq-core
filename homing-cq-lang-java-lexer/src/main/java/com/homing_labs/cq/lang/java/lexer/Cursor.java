package com.homing_labs.cq.lang.java.lexer;

import hue.captains.singapura.tao.ontology.Mutable;

import java.util.List;

/**
 * A place in a source text, moved forward over it a character at a time. It reads the text with its Unicode escapes
 * translated, and tells for each character where in the text as written it came from.
 */
final class Cursor implements Mutable {

    private final String text;

    /** Where each character of the text was written, and where the text ends; none when nothing was translated. */
    private final int[] offsets;

    private int at;

    Cursor(final String written, final List<Diagnostic> diagnostics) {
        if (written.indexOf('\\') < 0) {
            text = written;
            offsets = null;
        } else {
            final StringBuilder translated = new StringBuilder(written.length());
            offsets = new int[written.length() + 1];
            UnicodeEscapes.INSTANCE.translate(written, translated, offsets, diagnostics);
            text = translated.toString();
        }
    }

    boolean atEnd() {
        return at >= text.length();
    }

    /** The character here, or -1 at the end. */
    int peek() {
        return peek(0);
    }

    /** The character so many ahead of here, or -1 past the end. */
    int peek(final int ahead) {
        final int index = at + ahead;
        return index < text.length() ? text.charAt(index) : -1;
    }

    /** The code point here, which takes two characters when it lies outside the basic plane, or -1 at the end. */
    int codePoint() {
        return atEnd() ? -1 : text.codePointAt(at);
    }

    /** Whether the text goes on with these characters from here. */
    boolean matches(final String characters) {
        return text.startsWith(characters, at);
    }

    void advance() {
        advance(1);
    }

    void advance(final int count) {
        at = Math.min(text.length(), at + count);
    }

    void advanceCodePoint() {
        advance(Character.charCount(codePoint()));
    }

    /** Here, to come back to as the start of what is read next. */
    int mark() {
        return at;
    }

    /** The characters read since the mark. */
    String since(final int mark) {
        return text.substring(mark, at);
    }

    /** Where the character at the mark was written: its offset in the text as it was read. */
    int written(final int mark) {
        return offsets == null ? mark : offsets[mark];
    }
}
