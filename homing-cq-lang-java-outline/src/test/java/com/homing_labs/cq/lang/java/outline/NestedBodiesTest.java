package com.homing_labs.cq.lang.java.outline;

import com.homing_labs.cq.lang.java.lexer.JavaLexer;
import com.homing_labs.cq.lang.java.lexer.SourceText;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The body of a top-level type's member type, copied out of the file as a block of code and read on its own by the
 * nested class parser - one level down only.
 */
class NestedBodiesTest {

    private static final String OUTER = """
            class Outer {
                int x;

                /** A member. */
                static class Member {
                    /** Its count. */
                    private int count;
                    Member(int count) { this.count = count; }
                    void m() { }
                    class Deeper { int hidden; }
                }
            }
            """;

    @Test
    void aMemberTypesBodyIsReadFromABlockCopiedOutOfTheFile() {
        final Outline outline = outline(OUTER);
        final BodyPart.TypeDeclaration member = outline.types().getFirst().parts()
                .of(BodyPart.TypeDeclaration.class).getFirst();
        final NestedBody nested = member.nested().orElseThrow();
        assertEquals(outline.text(member.body()), nested.block().text(), "the body as written, braces and all");
        assertEquals(OUTER.indexOf("Member {") + "Member ".length(), nested.block().offset());
        assertEquals(5, nested.block().line());
        assertEquals(List.of(
                        "Field /** Its count. */\n        private int count;",
                        "Constructor Member(int count) { this.count = count; }",
                        "Method void m() { }",
                        "TypeDeclaration class Deeper { int hidden; }"),
                parts(nested));
        assertEquals(List.of(), outline.diagnostics().items());
    }

    @Test
    void whereAPartOfTheBlockIsInTheFileIsTheBlocksOffsetFurtherOn() {
        final NestedBody nested = outline(OUTER).types().getFirst().parts()
                .of(BodyPart.TypeDeclaration.class).getFirst().nested().orElseThrow();
        final BodyPart field = nested.parts().items().getFirst();
        assertEquals(OUTER.indexOf("/** Its count"), nested.start(field.range()));
        assertEquals(OUTER.indexOf("count;") + "count;".length(), nested.end(field.range()));
        assertEquals(OUTER.substring(nested.start(field.range()), nested.end(field.range())),
                nested.text(field.range()));
    }

    @Test
    void aMemberTypeOfAMemberTypeIsReadDownToItsHeaderAlone() {
        final NestedBody nested = outline(OUTER).types().getFirst().parts()
                .of(BodyPart.TypeDeclaration.class).getFirst().nested().orElseThrow();
        final BodyPart.TypeDeclaration deeper = nested.parts().of(BodyPart.TypeDeclaration.class).getFirst();
        assertEquals("Deeper", deeper.header().name().text());
        assertEquals("{ int hidden; }", nested.text(deeper.body()));
        assertEquals(Optional.empty(), deeper.nested(), "its body is left unread");
    }

    @Test
    void whatTheBlockHasWrongIsSaidInTheFileWhereItIs() {
        final Outline outline = outline("class A { interface I { { } int x; }"
                + " static class B { /** a */ /** b */ int y; } }");
        assertEquals(List.of("Error initializers not allowed in interfaces: {", "Error = expected: ;",
                        "Warning documentation comment is not attached to any declaration: /** a */"),
                diagnostics(outline));
        final NestedBody inInterface = outline.types().getFirst().parts().of(BodyPart.TypeDeclaration.class)
                .getFirst().nested().orElseThrow();
        assertEquals(List.of("{", ";"), inInterface.diagnostics().items().stream()
                .map(diagnostic -> inInterface.block().text().substring(diagnostic.start(), diagnostic.end()))
                .toList(), "and in the block, where it is there");
    }

    @Test
    void theParserReadsABlockWithTheWordItsTypeIsDeclaredWithAndItsName() {
        final NestedBody nested = NestedClassParser.INSTANCE.parse(TypeKeyword.Enum.INSTANCE, "E",
                new CodeBlock("{ A, B(2); E() { } E(int n) { } }", 100, 7));
        assertEquals(List.of("EnumConstant A,", "EnumConstant B(2);", "Constructor E() { }",
                "Constructor E(int n) { }"), parts(nested));
        assertEquals(102, nested.start(nested.parts().items().getFirst().range()));
        assertEquals(List.of("Field int x;"), parts(NestedClassParser.INSTANCE.parse(TypeKeyword.Class.INSTANCE,
                "C", new CodeBlock("{ int x;", 0, 1))), "a block never closed is read to its end");
    }

    private Outline outline(final String text) {
        return JavaOutliner.INSTANCE.outline(JavaLexer.INSTANCE.scan(new SourceText("", text)));
    }

    /** Each part as its kind and its text. */
    private List<String> parts(final NestedBody nested) {
        return nested.parts().items().stream()
                .map(part -> part.getClass().getSimpleName() + " " + nested.text(part.range()))
                .toList();
    }

    /** Each diagnostic as its severity, its message and the text it is about, in the file. */
    private List<String> diagnostics(final Outline outline) {
        final String text = outline.tokens().source().text();
        return outline.diagnostics().items().stream()
                .map(diagnostic -> diagnostic.severity().getClass().getSimpleName() + " " + diagnostic.message()
                        + ": " + text.substring(diagnostic.start(), diagnostic.end()))
                .toList();
    }
}
