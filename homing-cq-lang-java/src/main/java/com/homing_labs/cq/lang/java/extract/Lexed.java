package com.homing_labs.cq.lang.java.extract;

import com.homing_labs.cq.lang.java.lexer.Token;
import com.homing_labs.cq.lang.java.lexer.TokenStream;
import com.homing_labs.cq.lang.java.model.CodeBlocks;
import com.homing_labs.cq.lang.java.outline.CodeBlock;
import com.homing_labs.cq.lang.java.outline.DocComment;
import com.homing_labs.cq.lang.java.outline.TokenRange;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Tokens a class's parts were read from, and where their text starts in the file - its first character's offset and
 * line - with where each of the text's lines starts: the file's own tokens, or a member type's body's, lexed on its
 * own from a block copied out of the file. The code of their ranges is copied out of the file, where it is there.
 */
record Lexed(TokenStream tokens, int offset, int line, List<Integer> starts) {

    /** The line of the file a character of the text is on. */
    int lineOf(final int at) {
        int low = 0;
        int high = starts.size() - 1;
        while (low < high) {
            final int middle = (low + high + 1) / 2;
            if (starts.get(middle) <= at) {
                low = middle;
            } else {
                high = middle - 1;
            }
        }
        return line + low;
    }

    /** The code the tokens of a range are written with, copied out of the file, and where it is there. */
    CodeBlock block(final TokenRange range) {
        final int start = tokens.get(range.from()).start();
        final int end = tokens.get(range.to() - 1).end();
        return new CodeBlock(tokens.source().text().substring(start, end), offset + start, lineOf(start));
    }

    /** The code of the ranges, in the order written, each once. */
    CodeBlocks blocks(final List<TokenRange> ranges) {
        return new CodeBlocks(ranges.stream()
                .filter(range -> !range.isEmpty())
                .distinct()
                .sorted(Comparator.comparingInt(TokenRange::from))
                .map(this::block)
                .toList());
    }

    /** The code of a documentation comment, if there is one. */
    Optional<CodeBlock> doc(final Optional<DocComment> doc) {
        return doc.map(comment -> block(new TokenRange(comment.token(), comment.token() + 1)));
    }

    /**
     * A part's tokens without the comments after it, which its range takes in up to the next part's start - and, for
     * an enum constant, without the comma or the semicolon after it.
     */
    TokenRange trimmed(final TokenRange range, final boolean separated) {
        int to = range.to();
        while (to > range.from()) {
            final Token last = tokens.get(to - 1);
            final String text = tokens.text(last);
            if (!(last.kind().comment() || separated && (text.equals(",") || text.equals(";")))) {
                break;
            }
            to--;
        }
        return new TokenRange(range.from(), to);
    }

    /** The index of the first token after one that is no comment, or the end. */
    int following(final int at, final int end) {
        int next = at + 1;
        while (next < end && tokens.get(next).kind().comment()) {
            next++;
        }
        return next;
    }

    /** The text of the first token after one that is no comment, or nothing at the end. */
    String next(final int at, final int end) {
        final int next = following(at, end);
        return next < end ? tokens.text(tokens.get(next)) : "";
    }
}
