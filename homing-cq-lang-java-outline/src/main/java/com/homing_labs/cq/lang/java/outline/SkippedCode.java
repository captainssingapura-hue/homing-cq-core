package com.homing_labs.cq.lang.java.outline;

import hue.captains.singapura.tao.ontology.StatelessFunctionalObject;

/**
 * What the reading of a body skips, where a scan is: the code that follows a header - an initial value, an element's
 * default value, a block, the arguments an enum constant is made with - which is left unread for another stage; and
 * what cannot be read, up to where a part of a body can start again.
 */
record SkippedCode() implements StatelessFunctionalObject {

    static final SkippedCode INSTANCE = new SkippedCode();

    /**
     * A field's initial value as written, up to the comma or the semicolon that ends it, outside the brackets it opens.
     * Angle brackets holding only what type arguments hold are taken for type arguments, so a comma between them does
     * not end it: {@code new HashMap<String, Integer>()}.
     */
    TokenRange expression(final TokenScan scan) {
        final int start = scan.at();
        int depth = 0;
        while (scan.at() < scan.size() && !(depth == 0 && (scan.is(",") || scan.is(";")))) {
            if (scan.is("(") || scan.is("[") || scan.is("{")) {
                depth++;
            } else if (scan.is(")") || scan.is("]") || scan.is("}")) {
                if (depth == 0) {
                    break;
                }
                depth--;
            } else if (depth == 0 && scan.is("<")) {
                final int past = pastTypeArguments(scan, scan.at());
                if (past > scan.at()) {
                    while (scan.at() < past) {
                        scan.advance();
                    }
                    continue;
                }
            }
            scan.advance();
        }
        return new TokenRange(start, scan.at() == start ? start : scan.last() + 1);
    }

    /** An annotation element's default value as written, up to its semicolon, outside the brackets it opens. */
    TokenRange elementValue(final TokenScan scan) {
        final int start = scan.at();
        int depth = 0;
        while (scan.at() < scan.size() && !(depth == 0 && scan.is(";"))) {
            if (scan.is("(") || scan.is("[") || scan.is("{")) {
                depth++;
            } else if (scan.is(")") || scan.is("]") || scan.is("}")) {
                if (depth == 0) {
                    break;
                }
                depth--;
            }
            scan.advance();
        }
        if (scan.at() == start) {
            scan.error("illegal start of expression", scan.at(), Math.min(scan.at() + 1, scan.size()));
            return new TokenRange(start, start);
        }
        final TokenRange range = new TokenRange(start, scan.last() + 1);
        scan.unread.add(range);
        return range;
    }

    /** Steps over the braces here and all between them, which are left unread. */
    TokenRange block(final TokenScan scan) {
        final int open = scan.at();
        final TokenRange range = new TokenRange(open, scan.skipBlock() ? scan.last() + 1 : scan.size());
        scan.unread.add(range);
        return range;
    }

    /** Steps over the parentheses here and all between them, which are left unread. */
    TokenRange parenthesized(final TokenScan scan) {
        final int open = scan.at();
        scan.skipParentheses();
        final TokenRange range = new TokenRange(open, scan.last() + 1);
        scan.unread.add(range);
        return range;
    }

    /** Steps over the semicolon that ends a declaration; where there is none, over what cannot be read. */
    void semicolon(final TokenScan scan) {
        if (scan.is(";")) {
            scan.advance();
            return;
        }
        scan.error("';' expected", scan.at(), Math.min(scan.at() + 1, scan.size()));
        recover(scan);
    }

    /**
     * Steps over what cannot be read: up to the next semicolon and over it, over the next block, or up to the next
     * token a part can start with.
     */
    void recover(final TokenScan scan) {
        while (scan.at() < scan.size() && !startsPart(scan, scan.at())) {
            if (scan.is("{")) {
                scan.skipBlock();
                return;
            }
            if (scan.is(";")) {
                scan.advance();
                return;
            }
            scan.advance();
        }
    }

    /** Whether a part of a body can start at the token: an annotation, a type's keyword, or a modifier. */
    boolean startsPart(final TokenScan scan, final int index) {
        return scan.is(index, "@") || scan.is(index, "class") || scan.is(index, "interface") || scan.is(index, "enum")
                || scan.is(index, "record") && scan.identifier(scan.next(index))
                || scan.keyword(index) && scan.modifierWord(scan.text(index));
    }

    /**
     * The token after the type arguments the angle bracket at the index opens; the index itself, if what follows the
     * bracket is not all what type arguments hold, as a less-than's operands are not.
     */
    private int pastTypeArguments(final TokenScan scan, final int from) {
        int depth = 0;
        int index = from;
        while (index < scan.size()) {
            if (scan.is(index, "<")) {
                depth++;
            } else if (scan.is(index, ">")) {
                if (--depth == 0) {
                    return scan.next(index);
                }
            } else if (scan.is(index, "@")) {
                final int past = WrittenTypes.INSTANCE.pastAnnotations(scan, index);
                if (past == index) {
                    return from;
                }
                index = past;
                continue;
            } else if (!(scan.identifier(index) || scan.is(index, ".") || scan.is(index, ",") || scan.is(index, "?")
                    || scan.is(index, "&") || scan.is(index, "[") || scan.is(index, "]") || scan.is(index, "extends")
                    || scan.is(index, "super") || scan.keyword(index) && scan.primitive(scan.text(index)))) {
                return from;
            }
            index = scan.next(index);
        }
        return from;
    }
}
