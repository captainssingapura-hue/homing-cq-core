package com.homing_labs.cq.lang.java.outline;

import com.homing_labs.cq.lang.java.lexer.JavaLexer;
import com.homing_labs.cq.lang.java.lexer.SourceText;
import com.homing_labs.cq.lang.java.taxonomy.ClassKind;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JavaOutlinerTest {

    @Test
    void anOrdinaryFileIsItsPreamblePackageImportsAndTypesAndTheyCoverIt() {
        final Outline outline = outline("Box.java", """
                /* Licence. */
                package a.b;

                import java.util.List;
                // Between imports.
                import static java.util.Map.entry;

                /** A box. */
                public class Box {}
                ;
                class Other { void m() { } }
                // Trailing.
                """);
        assertEquals(List.of(
                        "Preamble /* Licence. */",
                        "PackageDeclaration package a.b;",
                        "ImportDeclaration import java.util.List;\n// Between imports.",
                        "ImportDeclaration import static java.util.Map.entry;",
                        "TypeDeclaration /** A box. */\npublic class Box {}",
                        "EmptyDeclaration ;",
                        "TypeDeclaration class Other { void m() { } }\n// Trailing."),
                parts(outline));
        assertEquals(List.of(), diagnostics(outline));
        assertCovered(outline);
        assertEquals("a.b", outline.packageDeclaration().orElseThrow().name().text());
        assertEquals("{ void m() { } }", outline.text(outline.types().get(1).body()));
    }

    @Test
    void eachKindOfImportIsReadWithTheNameItImportsBy() {
        final Outline outline = outline("", """
                import a.b.C;
                import a.b.*;
                import static a.b.C.m;
                import static a.b.C.*;
                import module java.base;
                import module.foo.Bar;
                """);
        assertEquals(List.of("SingleType a.b.C", "OnDemand a.b", "SingleStatic a.b.C.m", "StaticOnDemand a.b.C",
                        "Module java.base", "SingleType module.foo.Bar"),
                outline.imports().stream()
                        .map(declaration -> declaration.kind().getClass().getSimpleName() + " "
                                + declaration.name().text())
                        .toList());
        assertEquals(List.of(), diagnostics(outline));
    }

    @Test
    void aTypesHeaderIsReadInFullAndItsTypesAreKeptAsWritten() {
        final Outline outline = outline("Box.java", """
                /** A box. */
                @Deprecated public abstract sealed class Box<T extends Comparable<? super T>, @Tag U extends A & B>
                        extends java.util.AbstractList<java.util.Map.Entry<String, T[]>>
                        implements java.io.Serializable, Comparable<Box<T, U>> permits Big, Small {
                    { nested(); }
                }
                """);
        final TypeHeader header = outline.types().getFirst().header();
        assertTrue(header.doc().isPresent());
        assertEquals(List.of("public", "abstract", "sealed"),
                header.modifiers().words().stream().map(ModifierItem.Word::word).toList());
        assertEquals("Deprecated", header.modifiers().annotations().getFirst().name().text());
        assertEquals(TypeKeyword.Class.INSTANCE, header.keyword());
        assertEquals("Box", header.name().text());
        assertEquals(ClassKind.AbstractClass.INSTANCE, header.kind());
        assertEquals(List.of("T [Comparable<? super T>]", "U [A, B] @Tag"), header.typeParameters().items().stream()
                .map(parameter -> parameter.name().text() + " " + texts(outline, parameter.bounds().items())
                        + parameter.annotations().annotations().stream().map(tag -> " @" + tag.name().text())
                        .reduce("", String::concat))
                .toList());
        assertEquals(Optional.of("java.util.AbstractList<java.util.Map.Entry<String, T[]>>"),
                header.superclass().map(type -> outline.text(type.range())));
        assertEquals(List.of("java.io.Serializable", "Comparable<Box<T, U>>"),
                texts(outline, header.superinterfaces()));
        assertEquals(List.of("Big", "Small"), texts(outline, header.permitted()));
        assertEquals(Optional.empty(), header.components());
        assertEquals(List.of(), diagnostics(outline));
    }

    @Test
    void aRecordsComponentsAreReadWithTheirAnnotationsTypesAndNames() {
        final Outline outline = outline("", """
                record Point<T>(@Tag int x, java.util.List<java.util.Map<String, T>> ys, String @Tag ... rest)
                        implements Runnable {}
                record Empty() {}
                """);
        final List<FilePart.TypeDeclaration> types = outline.types();
        assertEquals(List.of("@Tag int x", "java.util.List<java.util.Map<String, T>> ys",
                        "String @Tag, any number of, rest"),
                types.getFirst().header().components().orElseThrow().items().stream()
                        .map(component -> component.annotations().annotations().stream()
                                .map(tag -> "@" + tag.name().text() + " ").reduce("", String::concat)
                                + outline.text(component.type().range())
                                + (component.variable() ? ", any number of, " : " ") + component.name().text())
                        .toList());
        assertEquals(List.of("Runnable"), texts(outline, types.getFirst().header().superinterfaces()));
        assertEquals(Optional.of(List.of()),
                types.getLast().header().components().map(RecordComponents::items));
        assertEquals(List.of(), diagnostics(outline));
    }

    @Test
    void eachWordATypeIsDeclaredWithIsRead() {
        final Outline outline = outline("", """
                interface I extends A, B {}
                enum E implements I { X, Y }
                @interface N {}
                @ interface M {}
                record R() {}
                non-sealed class C extends D {}
                abstract interface J {}
                """);
        assertEquals(List.of("interface I Interface", "enum E Enum", "@interface N Annotation",
                        "@interface M Annotation", "record R Record", "class C Class", "interface J Interface"),
                outline.types().stream()
                        .map(type -> type.header().keyword().word() + " " + type.header().name().text() + " "
                                + type.header().kind().getClass().getSimpleName())
                        .toList());
        assertEquals(List.of("A", "B"), texts(outline, outline.types().getFirst().header().superinterfaces()));
        assertEquals(List.of("non-sealed"), outline.types().get(5).header().modifiers().words().stream()
                .map(ModifierItem.Word::word).toList());
        assertEquals(List.of(), diagnostics(outline));
    }

    @Test
    void aBodyIsClosedLeftOpenOrMissing() {
        final Outline closed = outline("", "class A { { } \"}\" '}' /* } */ }");
        assertEquals(BodyEnd.Closed.INSTANCE, closed.types().getFirst().end());
        assertEquals("{ { } \"}\" '}' /* } */ }", closed.text(closed.types().getFirst().body()));

        final Outline open = outline("", "class A { void m() {");
        assertEquals(BodyEnd.Unterminated.INSTANCE, open.types().getFirst().end());
        assertEquals("{ void m() {", open.text(open.types().getFirst().body()));
        assertEquals(List.of("Error reached end of file while parsing: the body of A is never closed: {"),
                diagnostics(open));

        final Outline missing = outline("", "class A\nclass B {}");
        assertEquals(BodyEnd.Missing.INSTANCE, missing.types().getFirst().end());
        assertEquals(List.of("A", "B"), missing.types().stream().map(type -> type.header().name().text()).toList());
        assertEquals(List.of("Error '{' expected: class"), diagnostics(missing));
    }

    @Test
    void aDocumentationCommentBelongsToTheDeclarationRightAfterIt() {
        final Outline outline = outline("", """
                /** Dangling: no import takes one. */
                import a.B;
                /** Dangling: another follows. */
                /** Taken. */
                /* Plain. */
                class A {}
                @Deprecated
                /** Dangling: after the annotation. */
                class B { /** Left to the body. */ }
                /** After the last declaration. */
                """);
        assertEquals(Optional.of("/** Taken. */"), outline.types().getFirst().header().doc()
                .map(doc -> outline.text(new TokenRange(doc.token(), doc.token() + 1))));
        assertEquals(Optional.empty(), outline.types().getLast().header().doc());
        assertEquals(List.of(
                        "Warning documentation comment is not attached to any declaration: /** Dangling: no import"
                                + " takes one. */",
                        "Warning documentation comment is not attached to any declaration: /** Dangling: another"
                                + " follows. */",
                        "Warning documentation comment is not attached to any declaration: /** Dangling: after the"
                                + " annotation. */"),
                diagnostics(outline));
        assertEquals("/** Taken. */\n/* Plain. */\nclass A {}", outline.text(outline.types().getFirst().range()));
    }

    @Test
    void onlyPackageInfoGivesItsPackageADocumentationCommentAndAnnotations() {
        final String text = "/** The package. */\n@Deprecated\npackage a;\n";
        final Outline info = outline("package-info.java", text);
        assertTrue(info.packageDeclaration().orElseThrow().doc().isPresent());
        assertEquals(List.of(), diagnostics(info));

        final Outline other = outline("A.java", text);
        assertEquals(Optional.empty(), other.packageDeclaration().orElseThrow().doc());
        assertEquals(List.of(
                        "Warning documentation comment is not attached to any declaration: /** The package. */",
                        "Error package annotations should be in file package-info.java: @Deprecated"),
                diagnostics(other));
    }

    @Test
    void anotherFormOfFileIsSaidNotToBeReadYet() {
        final Outline module = outline("module-info.java",
                "import a.B;\n/** The module. */\n@Deprecated open module m { requires x; }");
        assertEquals(List.of("Error a module declaration is not read yet: only ordinary compilation units are: open"),
                diagnostics(module));
        assertEquals(List.of("ImportDeclaration import a.B;",
                        "Unrecognised /** The module. */\n@Deprecated open module m { requires x; }"),
                parts(module));

        final Outline compact = outline("Main.java", "void main() { /** Inside. */ }\nclass A {}");
        assertEquals(List.of("Error a method or a field outside any class is not read yet: only ordinary"
                + " compilation units are: void"), diagnostics(compact));
        assertCovered(compact);
    }

    @Test
    void whatCannotBeReadIsSkippedToTheNextPart() {
        final Outline outline = outline("", "} x y; class A {} 42 { ; } class B {}");
        assertEquals(List.of("Unrecognised } x y;", "TypeDeclaration class A {}", "Unrecognised 42 { ; }",
                "TypeDeclaration class B {}"), parts(outline));
        assertEquals(List.of("Error class, interface, enum, or record expected: } x y;",
                "Error class, interface, enum, or record expected: 42 { ; }"), diagnostics(outline));
    }

    @Test
    void whatTheGrammarAsksOfAFileIsSaidWhereItIsNotDone() {
        assertEquals(List.of("Error extraneous semicolon: ;"), diagnostics(outline("", "import a.B; ; import c.D;")));
        assertEquals(List.of("Error extraneous semicolon: ;"), diagnostics(outline("", ";\nimport a.B;")));
        assertEquals(List.of("Error extraneous semicolon: ;"), diagnostics(outline("", "package p;; import a.B;")));
        assertEquals(List.of(), diagnostics(outline("", "import a.B;;\nclass X {}")));
        assertEquals(List.of("Error the imports come before the types: import"),
                diagnostics(outline("", "class A {} import a.B;")));
        assertEquals(List.of("Error the package declaration comes before the imports and the types: package"),
                diagnostics(outline("", "import a.B; package p;")));
        assertEquals(List.of("Error a file declares one package only: package"),
                diagnostics(outline("", "package p; package q;")));
        assertEquals(List.of("Error modifier public not allowed here: public"),
                diagnostics(outline("", "public package p;")));
        assertEquals(List.of("Error an import takes no modifiers and no annotations: public"),
                diagnostics(outline("", "public import a.B;")));
        assertEquals(List.of("Error '.' expected: a"), diagnostics(outline("", "import a;")));
        assertEquals(List.of("Error ';' expected: class"), diagnostics(outline("", "import a.B\nclass X {}")));
        assertEquals(List.of("Error <identifier> expected: ;"), diagnostics(outline("", "package ;")));
    }

    @Test
    void whatTheGrammarAsksOfATypesHeaderIsSaidWhereItIsNotDone() {
        assertEquals(List.of("Error modifier static not allowed here: static"),
                diagnostics(outline("", "static class F {}")));
        assertEquals(List.of("Error repeated modifier: public"),
                diagnostics(outline("M.java", "public public class M {}")));
        assertEquals(List.of("Error illegal combination of modifiers: abstract and final: abstract final class X"),
                diagnostics(outline("", "abstract final class X {}")));
        assertEquals(List.of("Error modifier final not allowed here: final"),
                diagnostics(outline("", "final enum E { A }")));
        assertEquals(List.of("Error modifier sealed not allowed here: sealed"),
                diagnostics(outline("", "sealed enum E { A }")));
        assertEquals(List.of("Error modifier abstract not allowed here: abstract"),
                diagnostics(outline("", "abstract record R() {}")));
        assertEquals(List.of("Error modifier final not allowed here: final"),
                diagnostics(outline("", "final interface I {}")));
        assertEquals(List.of("Error modifier transient not allowed here: transient"),
                diagnostics(outline("", "transient class T {}")));
        assertEquals(List.of("Warning strictfp is not required: since Java 17 all floating-point arithmetic is"
                + " strict: strictfp"), diagnostics(outline("", "strictfp class S {}")));
        assertEquals(List.of("Error non-sealed is written without spaces: non - sealed"),
                diagnostics(outline("", "non - sealed class D extends B {}")));
        assertEquals(List.of("Error 'record' is a restricted name: it cannot name a type: record"),
                diagnostics(outline("", "class record {}")));
        assertEquals(List.of("Error an enum takes no type parameters: T"), diagnostics(outline("", "enum E<T> {}")));
        assertEquals(List.of("Error a record declares its components, in parentheses after its name: R"),
                diagnostics(outline("", "record R implements Runnable {}")));
        assertEquals(List.of("Error only a record has components: C"), diagnostics(outline("", "class C(int x) {}")));
        assertEquals(List.of("Error an enum has no extends clause: extends Object"),
                diagnostics(outline("", "enum E extends Object { A }")));
        assertEquals(List.of("Error an interface has no implements clause: implements J"),
                diagnostics(outline("", "interface I implements J {}")));
        assertEquals(List.of("Error the extends clause comes before the implements clause: extends D"),
                diagnostics(outline("", "class C implements I extends D {}")));
        assertEquals(List.of("Error a second extends clause: extends B"),
                diagnostics(outline("", "class C extends A extends B {}")));
        assertEquals(List.of("Error a class extends one class only: extends A, B"),
                diagnostics(outline("", "class C extends A, B {}")));
        assertEquals(List.of("Error class N3 is public, should be declared in a file named N3.java: N3"),
                diagnostics(outline("N1.java", "class N1 {} class N2 {} public class N3 {}")));
        assertEquals(List.of("Error '>' expected: B<C"), diagnostics(outline("", "class A extends B<C {}")));
        assertEquals(List.of("Error <identifier> expected: >"), diagnostics(outline("", "class A<> {}")));
    }

    /** Every token of the file is in exactly one part, and the parts are in order. */
    private void assertCovered(final Outline outline) {
        int end = 0;
        for (final FilePart part : outline.parts().items()) {
            assertEquals(end, part.range().from(), part.toString());
            assertTrue(part.range().to() > part.range().from(), part.toString());
            end = part.range().to();
        }
        assertEquals(outline.tokens().size(), end);
    }

    private Outline outline(final String fileName, final String text) {
        return JavaOutliner.INSTANCE.outline(JavaLexer.INSTANCE.scan(new SourceText(fileName, text)));
    }

    /** Each part as its kind and its text, trailing whitespace left out. */
    private List<String> parts(final Outline outline) {
        return outline.parts().items().stream()
                .map(part -> part.getClass().getSimpleName() + " " + outline.text(part.range()))
                .toList();
    }

    /** Each diagnostic as its severity, its message and the text it is about. */
    private List<String> diagnostics(final Outline outline) {
        final String text = outline.tokens().source().text();
        return outline.diagnostics().items().stream()
                .map(diagnostic -> diagnostic.severity().getClass().getSimpleName() + " " + diagnostic.message()
                        + ": " + text.substring(diagnostic.start(), diagnostic.end()))
                .toList();
    }

    private List<String> texts(final Outline outline, final List<TypeText> types) {
        return types.stream().map(type -> outline.text(type.range())).toList();
    }
}
