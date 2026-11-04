package com.homing_labs.cq.rules.basic.enforce;

import com.homing_labs.cq.lang.java.lexer.JavaLexer;
import com.homing_labs.cq.lang.java.lexer.SourceText;
import com.homing_labs.cq.lang.java.lexer.Token;
import com.homing_labs.cq.lang.java.lexer.TokenKind;
import com.homing_labs.cq.lang.java.lexer.TokenStream;
import hue.captains.singapura.tao.ontology.StatelessFunctionalObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Counts a source file's lines as the size rules do: each line by its code - from the line's start to its last
 * character of code, so its indentation counts and a comment after the code does not - in lines of at most 120
 * characters, {@code ceiling(length / 120)}. A blank line, or one of comments alone, counts none. The file is read with
 * the project's lexer, which tells code from comments; a text block's lines are code to their ends.
 */
public record EffectiveLines() implements StatelessFunctionalObject {

    public static final EffectiveLines INSTANCE = new EffectiveLines();

    /** How long an effective line is, at most. */
    private static final int WIDTH = 120;

    /** Each line of a source file, from its first, in effective lines. */
    public LineCounts of(final String text) {
        final boolean[] code = new boolean[text.length()];
        final TokenStream tokens = JavaLexer.INSTANCE.scan(new SourceText("", text));
        for (int at = 0; at < tokens.size(); at++) {
            final Token token = tokens.get(at);
            if (!comment(token.kind())) {
                for (int character = token.start(); character < token.end(); character++) {
                    code[character] = true;
                }
            }
        }
        final List<LineCount> counts = new ArrayList<>();
        int start = 0;
        int last = -1;
        for (int at = 0; at <= text.length(); at++) {
            final boolean ends = at == text.length() || text.charAt(at) == '\n' || text.charAt(at) == '\r';
            if (!ends) {
                if (code[at]) {
                    last = at;
                }
                continue;
            }
            counts.add(new LineCount(last < start ? 0 : (last - start + WIDTH) / WIDTH));
            if (at < text.length() && text.charAt(at) == '\r' && at + 1 < text.length()
                    && text.charAt(at + 1) == '\n') {
                at++;
            }
            start = at + 1;
            last = -1;
        }
        return new LineCounts(counts);
    }

    private boolean comment(final TokenKind kind) {
        return kind instanceof TokenKind.LineComment || kind instanceof TokenKind.BlockComment
                || kind instanceof TokenKind.DocComment;
    }
}
