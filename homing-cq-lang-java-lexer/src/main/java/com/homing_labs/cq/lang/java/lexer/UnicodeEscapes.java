package com.homing_labs.cq.lang.java.lexer;

import hue.captains.singapura.tao.ontology.StatelessFunctionalObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Translates the Unicode escapes of a text - a backslash, one or more {@code u}, four hexadecimal digits - into the
 * characters they stand for, as Java does before reading anything else (JLS 3.3). A backslash begins an escape only
 * when an even number of backslashes come right before it, so one escaped in a string literal begins none.
 */
record UnicodeEscapes() implements StatelessFunctionalObject {

    static final UnicodeEscapes INSTANCE = new UnicodeEscapes();

    private static final String HEX_DIGITS = "0123456789abcdefABCDEF";

    /** The text with its escapes translated. */
    String translate(final String written) {
        final StringBuilder text = new StringBuilder(written.length());
        translate(written, text, new int[written.length() + 1], new ArrayList<>());
        return text.toString();
    }

    /**
     * Translates the written text into {@code text}, noting in {@code offsets} where each character of it was written,
     * and where the text ends, and in {@code diagnostics} any escape with fewer than four hexadecimal digits.
     */
    void translate(final String written, final StringBuilder text, final int[] offsets,
                   final List<Diagnostic> diagnostics) {
        int backslashes = 0;
        int at = 0;
        while (at < written.length()) {
            final char c = written.charAt(at);
            if (c == '\\' && backslashes % 2 == 0 && at + 1 < written.length() && written.charAt(at + 1) == 'u') {
                int digits = at + 1;
                while (digits < written.length() && written.charAt(digits) == 'u') {
                    digits++;
                }
                final int value = hex(written, digits);
                if (value >= 0) {
                    offsets[text.length()] = at;
                    text.append((char) value);
                    at = digits + 4;
                    backslashes = 0;
                    continue;
                }
                diagnostics.add(new Diagnostic(Severity.Error.INSTANCE, "illegal unicode escape", at,
                        Math.min(digits + 4, written.length())));
            }
            offsets[text.length()] = at;
            text.append(c);
            backslashes = c == '\\' ? backslashes + 1 : 0;
            at++;
        }
        offsets[text.length()] = written.length();
    }

    /** The value of the four hexadecimal digits from the index on, or -1 if there are not four. */
    private int hex(final String written, final int from) {
        if (from + 4 > written.length()) {
            return -1;
        }
        int value = 0;
        for (int at = from; at < from + 4; at++) {
            final int digit = HEX_DIGITS.indexOf(written.charAt(at));
            if (digit < 0) {
                return -1;
            }
            value = value * 16 + (digit < 16 ? digit : digit - 6);
        }
        return value;
    }
}
