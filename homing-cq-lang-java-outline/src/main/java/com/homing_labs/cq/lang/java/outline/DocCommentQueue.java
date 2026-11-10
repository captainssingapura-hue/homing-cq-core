package com.homing_labs.cq.lang.java.outline;

import com.homing_labs.cq.lang.java.lexer.TokenKind;
import com.homing_labs.cq.lang.java.lexer.TokenStream;
import hue.captains.singapura.tao.ontology.Mutable;
import hue.captains.singapura.tao.ontology.Scalar;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * The documentation comments of a file, kept as javac keeps them to say which no declaration takes (JavacParser's
 * dangling comments): each joins a queue, and the queue is emptied where a declaration's header ends - of its own
 * comment, and of the others, each said to be attached to no declaration. An initializer empties it without a word, as
 * javac gives an initializer no comment; what is left in the queue when the file ends is not said either.
 *
 * <p>javac misses a comment before a token it has read ahead - its scanner queues one only when it reads the token
 * after it in turn - as the token after an enum constant's name is. Those are said here.</p>
 */
final class DocCommentQueue implements Mutable {

    private final List<Emptying> emptyings = new ArrayList<>();

    /** A declaration's header ends before the token: the queue is emptied, and all in it but its own is said. */
    void flush(final int token, final Optional<DocComment> own) {
        emptyings.add(new Emptying(token, own.map(DocComment::token).orElse(-1), true));
    }

    /** The queue is emptied before the token, and nothing in it said. */
    void clear(final int token) {
        emptyings.add(new Emptying(token, -1, false));
    }

    /** The documentation comments said to be attached to no declaration, but none in what was left unread. */
    List<Integer> dangling(final TokenStream stream, final List<TokenRange> unread) {
        final List<Emptying> ordered = emptyings.stream().sorted(Comparator.comparingInt(Emptying::token)).toList();
        final BitSet left = new BitSet(stream.size());
        unread.forEach(range -> left.set(range.from(), Math.max(range.from(), range.to())));
        final List<Integer> dangling = new ArrayList<>();
        int next = 0;
        for (int token = 0; token < stream.size(); token++) {
            if (!(stream.get(token).kind() instanceof TokenKind.DocComment) || left.get(token)) {
                continue;
            }
            while (next < ordered.size() && ordered.get(next).token() <= token) {
                next++;
            }
            if (next < ordered.size() && ordered.get(next).said() && ordered.get(next).own() != token) {
                dangling.add(token);
            }
        }
        return dangling;
    }

    /** The queue emptied before a token: of the comment a declaration takes, if any, and saying the others or not. */
    private record Emptying(int token, int own, boolean said) implements Scalar {
    }
}
