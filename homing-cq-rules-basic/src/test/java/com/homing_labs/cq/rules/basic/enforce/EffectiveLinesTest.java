package com.homing_labs.cq.rules.basic.enforce;

import com.homing_labs.cq.lang.java.outline.CodeBlock;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EffectiveLinesTest {

    @Test
    void aLineCountsByItsCodeFromItsStartBlankLinesAndCommentsCountingNone() {
        final String text = String.join("\n",
                "/** A documentation comment. */",
                "class A {",
                "",
                "    // a line comment",
                "    int a; // a comment after the code is not counted",
                "    /* a block",
                "       comment */",
                "    int f = " + "1 + ".repeat(20) + "1; // " + "x".repeat(60),
                "}");
        assertEquals(List.of(0, 1, 0, 0, 1, 0, 0, 1, 1), counts(text));
    }

    @Test
    void aLineLongerThan120CharactersCountsAsManyAsItsLengthTakes() {
        final String text = String.join("\n",
                "    int c = " + "1 + ".repeat(26) + "111;",
                "    int d = " + "1 + ".repeat(26) + "1111;",
                "    int e = " + "1 + ".repeat(56) + "111;",
                "    int g = " + "1 + ".repeat(56) + "1111;");
        assertEquals(List.of(120, 121, 240, 241), text.lines().map(String::length).toList());
        assertEquals(List.of(1, 2, 2, 3), counts(text));
    }

    @Test
    void aTextBlocksLinesAreCodeAndAnyLineEndingEndsALine() {
        final String text = String.join("\r\n",
                "    String t = \"\"\"",
                "        a line of the text block",
                "        \"\"\";",
                "    int a;") + "\r" + "    int b;";
        assertEquals(List.of(1, 1, 1, 1, 1), counts(text));
    }

    @Test
    void aBlockCountsTheEffectiveLinesOfEveryLineItIsOn() {
        final LineCounts counts = new LineCounts(Stream.of(0, 1, 0, 2, 1).map(LineCount::new).toList());
        assertEquals(3, counts.of(new CodeBlock("x\ny\nz", 0, 2)), "lines 2 to 4");
        assertEquals(2, counts.of(new CodeBlock("x", 0, 4)));
    }

    private List<Integer> counts(final String text) {
        return EffectiveLines.INSTANCE.of(text).items().stream().map(LineCount::effectiveLines).toList();
    }
}
