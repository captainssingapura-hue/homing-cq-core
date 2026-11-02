package com.homing_labs.cq.lang.java.outline;

import hue.captains.singapura.tao.ontology.StatelessFunctionalObject;

/**
 * Types as a member's header writes them, read where a scan is: a type in full, the brackets after a type or a name,
 * and the annotations that may be written before either. Type arguments are stepped over.
 */
record WrittenTypes() implements StatelessFunctionalObject {

    static final WrittenTypes INSTANCE = new WrittenTypes();

    /**
     * A type as written, from the token here (JLS 4.1): a primitive type or void, or a class type - names with dots
     * between them, each perhaps annotated and given type arguments - and brackets after it, each pair perhaps
     * annotated. An empty range, and a diagnostic, where no type starts.
     */
    TypeText type(final TokenScan scan) {
        final int start = scan.at();
        if (scan.keyword(scan.at()) && (scan.primitive(scan.text(scan.at())) || scan.is("void"))) {
            scan.advance();
        } else if (scan.identifier(scan.at())) {
            scan.advance();
            while (true) {
                if (scan.is("<")) {
                    typeArguments(scan);
                } else if (scan.is(".") && scan.identifier(pastAnnotations(scan, scan.next(scan.at())))) {
                    scan.advance();
                    while (scan.is("@")) {
                        scan.annotation();
                    }
                    scan.advance();
                } else {
                    break;
                }
            }
        } else {
            scan.error("<identifier> expected", scan.at(), Math.min(scan.at() + 1, scan.size()));
            return new TypeText(new TokenRange(start, start));
        }
        dimensions(scan);
        return new TypeText(new TokenRange(start, scan.last() + 1));
    }

    /** Whether a type can start at the token: a name, a primitive type, or void. */
    boolean typeStart(final TokenScan scan, final int index) {
        return scan.identifier(index)
                || scan.keyword(index) && (scan.primitive(scan.text(index)) || scan.is(index, "void"));
    }

    /** Brackets written after a type or a name, each pair perhaps annotated: an empty range where there are none. */
    TokenRange dimensions(final TokenScan scan) {
        final int start = scan.at();
        while (scan.is(pastAnnotations(scan, scan.at()), "[")
                && scan.is(scan.next(pastAnnotations(scan, scan.at())), "]")) {
            while (scan.is("@")) {
                scan.annotation();
            }
            scan.advance();
            scan.advance();
        }
        return new TokenRange(start, scan.at() == start ? start : scan.last() + 1);
    }

    /** The token after the annotations written from the index on: the index itself, when none is. */
    int pastAnnotations(final TokenScan scan, final int from) {
        int index = from;
        while (scan.is(index, "@") && !scan.is(scan.next(index), "interface")) {
            index = scan.next(index);
            while (scan.identifier(index) && scan.is(scan.next(index), ".")
                    && scan.identifier(scan.next(scan.next(index)))) {
                index = scan.next(scan.next(index));
            }
            if (scan.identifier(index)) {
                index = scan.next(index);
            }
            if (scan.is(index, "(")) {
                int depth = 0;
                while (index < scan.size() && !scan.is(index, ";")) {
                    if (scan.is(index, "(")) {
                        depth++;
                    } else if (scan.is(index, ")") && --depth == 0) {
                        index = scan.next(index);
                        break;
                    }
                    index = scan.next(index);
                }
            }
        }
        return index;
    }

    /** Steps over type arguments, in their angle brackets; a diagnostic where none closes them before a type ends. */
    private void typeArguments(final TokenScan scan) {
        final int open = scan.at();
        int depth = 0;
        while (scan.at() < scan.size() && !scan.is(";") && !scan.is("{") && !scan.is("=") && !scan.is(")")) {
            if (scan.is("@")) {
                scan.annotation();
                continue;
            }
            if (scan.is("<")) {
                depth++;
            } else if (scan.is(">") && --depth == 0) {
                scan.advance();
                return;
            }
            scan.advance();
        }
        scan.error("'>' expected", open, scan.last() + 1);
    }
}
