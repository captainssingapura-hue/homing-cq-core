package com.homing_labs.cq.bench;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** How the rule book writes a documentation comment as plain text. */
class DocTextTest {

    @Test
    void aOneLineCommentIsItsTextWithALinkAsTheNameItLinksTo() {
        assertEquals("Encapsulation in Java: a class declares no public instance field.",
                DocText.INSTANCE.of("/** {@link Encapsulation} in Java: a class declares no public instance field. */"));
    }

    @Test
    void eachParagraphIsOnOneLineAndTheBlockTagsAreLeftOut() {
        assertEquals("""
                        A rule in two paragraphs, written over two lines.

                        The second says Map<K, V> as code, a < b in HTML, and nothing of its parameters.""",
                DocText.INSTANCE.of("""
                        /**
                         * A rule in two paragraphs,
                         *     written over two lines.
                         *
                         * <p>The second says {@code Map<K, V>} as code, a &lt; b in HTML,
                         * and nothing of its parameters.</p>
                         *
                         * @param limit the most it allows
                         */"""));
    }

    @Test
    void aLinkShowsItsLabelOrElseWhatItRefersToWithoutPackages() {
        assertEquals("Gate, Scanner.scan(List, ClassLoader), limit(), the scan, and size.",
                DocText.INSTANCE.of("/** {@link com.homing_labs.cq.scan.Gate}, {@link Scanner#scan(List, "
                        + "ClassLoader)}, {@link #limit()}, {@linkplain Scanner the scan}, and {@code size}. */"));
    }

    @Test
    void aListHasAParagraphForEachItem() {
        assertEquals("It checks:\n\n- fields;\n\n- methods.",
                DocText.INSTANCE.of("/** It checks: <ul><li>fields;</li><li>methods.</li></ul> */"));
    }
}
