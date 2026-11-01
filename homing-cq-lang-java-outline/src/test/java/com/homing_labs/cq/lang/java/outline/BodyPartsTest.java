package com.homing_labs.cq.lang.java.outline;

import com.homing_labs.cq.lang.java.lexer.JavaLexer;
import com.homing_labs.cq.lang.java.lexer.SourceText;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BodyPartsTest {

    @Test
    void aTypesBodyIsItsPartsAndTheyCoverItFromTheFirstToTheClosingBrace() {
        final Outline outline = outline("""
                class Box<T> {
                    // Before the first part: the type's own.
                    /** The count. */
                    private int count;
                    static { load(); }
                    { count = 1; }
                    /** Makes one. */
                    Box(T item) { this.item = item; }
                    ;
                    <R> R map(java.util.function.Function<T, R> f) { return f.apply(item); }
                    // After the method.
                    static final class Inner { int hidden; }
                }
                """);
        final FilePart.TypeDeclaration type = outline.types().getFirst();
        assertEquals(List.of(
                        "Field /** The count. */\n    private int count;",
                        "Initializer static { load(); }",
                        "Initializer { count = 1; }",
                        "Constructor /** Makes one. */\n    Box(T item) { this.item = item; }",
                        "EmptyDeclaration ;",
                        "Method <R> R map(java.util.function.Function<T, R> f) { return f.apply(item); }\n"
                                + "    // After the method.",
                        "TypeDeclaration static final class Inner { int hidden; }"),
                parts(outline, type));
        assertEquals(List.of(), diagnostics(outline));
        assertCovered(outline, type);
        assertEquals("{\n    // Before the first part: the type's own.",
                outline.text(new TokenRange(type.body().from(), type.parts().items().getFirst().range().from())));
        assertEquals("{ int hidden; }", outline.text(type.parts().of(BodyPart.TypeDeclaration.class).getFirst()
                .body()));
    }

    @Test
    void aFieldDeclaresEachOfItsVariablesWithTheBracketsAfterItAndItsInitialValue() {
        final Outline outline = outline("""
                class F {
                    int a, b[] = {1, 2}, c = f(x, y);
                    java.util.Map<String, Integer> m = new java.util.HashMap<String, Integer>(), n = null;
                    boolean p = i < j, q = k > l;
                    Runnable r = () -> { }, s = this::run;
                }
                """);
        assertEquals(List.of(
                        "int: a | b [] = {1, 2} | c = f(x, y)",
                        "java.util.Map<String, Integer>: m = new java.util.HashMap<String, Integer>() | n = null",
                        "boolean: p = i < j | q = k > l",
                        "Runnable: r = () -> { } | s = this::run"),
                outline.types().getFirst().parts().of(BodyPart.Field.class).stream()
                        .map(field -> outline.text(field.type().range()) + ": " + field.declarators().items().stream()
                                .map(declarator -> declarator.name().text()
                                        + (declarator.dimensions().isEmpty() ? ""
                                        : " " + outline.text(declarator.dimensions()))
                                        + declarator.initializer().map(value -> " = " + outline.text(value))
                                        .orElse(""))
                                .collect(Collectors.joining(" | ")))
                        .toList());
        assertEquals(List.of(), diagnostics(outline));
    }

    @Test
    void aMethodsHeaderIsReadInFull() {
        final Outline outline = outline("""
                abstract class M {
                    /** Maps. */
                    @Deprecated public final <K extends Comparable<K>, V> java.util.Map<K, V>[] map(final @Tag K key,
                            V @Tag ... values) throws java.io.IOException, RuntimeException { return null; }
                    abstract int legacy(int[] grid[])[];
                    void receive(M this, int x) {}
                }
                """);
        final List<BodyPart.Method> methods = outline.types().getFirst().parts().of(BodyPart.Method.class);
        final BodyPart.Method map = methods.getFirst();
        assertTrue(map.doc().isPresent());
        assertEquals(List.of("public", "final"), words(map.modifiers()));
        assertEquals("Deprecated", map.modifiers().annotations().getFirst().name().text());
        assertEquals(List.of("K [Comparable<K>]", "V []"), map.typeParameters().items().stream()
                .map(parameter -> parameter.name().text() + " " + texts(outline, parameter.bounds().items()))
                .toList());
        assertEquals("java.util.Map<K, V>[]", outline.text(map.result().range()));
        assertEquals("map", map.name().text());
        assertEquals(List.of("final @Tag K key", "V @Tag, any number of, values"), formal(outline, map.parameters()));
        assertEquals(List.of("java.io.IOException", "RuntimeException"), texts(outline, map.thrown().items()));
        assertEquals(Optional.of("{ return null; }"), map.body().map(outline::text));

        final BodyPart.Method legacy = methods.get(1);
        assertEquals("int", outline.text(legacy.result().range()));
        assertEquals("[]", outline.text(legacy.dimensions()));
        assertEquals(List.of("int[] grid []"), formal(outline, legacy.parameters()));
        assertEquals(Optional.empty(), legacy.body());

        final BodyPart.Method receive = methods.get(2);
        assertEquals(Optional.of("M this"), receive.parameters().receiver()
                .map(receiver -> outline.text(receiver.range())));
        assertEquals(List.of("int x"), formal(outline, receive.parameters()));
        assertEquals(List.of(), diagnostics(outline));

        final Outline inner = outline("class Inner { Inner(Outer Outer.this) {} }");
        assertEquals(Optional.of("Outer.this"), inner.types().getFirst().parts().of(BodyPart.Constructor.class)
                .getFirst().parameters().receiver().map(receiver -> receiver.name().text()));
    }

    @Test
    void aBraceWrittenWithAUnicodeEscapeIsABraceStill() {
        final Outline outline = outline("class A \\u007B int x; \\u007D");
        assertEquals(List.of("Field int x;"), parts(outline, outline.types().getFirst()));
        assertEquals(List.of(), diagnostics(outline));
    }

    @Test
    void aConstructorIsNamedAsItsClassIsAndARecordCanHaveACompactOne() {
        final Outline outline = outline("""
                class C {
                    <T> C(T seed) throws Exception { }
                }
                record R(int x) {
                    static int count;
                    R { count++; }
                    R(int x, int y) { this(x + y); }
                }
                """);
        final BodyPart.Constructor generic = outline.types().getFirst().parts().of(BodyPart.Constructor.class)
                .getFirst();
        assertEquals("C", generic.name().text());
        assertEquals("T", generic.typeParameters().items().getFirst().name().text());
        assertEquals(List.of("T seed"), formal(outline, generic.parameters()));
        assertEquals(List.of("Exception"), texts(outline, generic.thrown().items()));

        final FilePart.TypeDeclaration record = outline.types().getLast();
        assertEquals(List.of("Field", "CompactConstructor", "Constructor"), record.parts().items().stream()
                .map(part -> part.getClass().getSimpleName()).toList());
        final BodyPart.CompactConstructor compact = record.parts().of(BodyPart.CompactConstructor.class).getFirst();
        assertEquals("R", compact.name().text());
        assertEquals("{ count++; }", outline.text(compact.body()));
        assertEquals(List.of(), diagnostics(outline));
    }

    @Test
    void anEnumsConstantsComeBeforeItsMembersAndTheSemicolonAfterThemIsTheLastOnes() {
        final Outline outline = outline("""
                enum Planet {
                    /** The first. */
                    @Deprecated MERCURY(3.3e23),
                    VENUS(4.8e24) { double gravity() { return 8.9; } },
                    EARTH,
                    ;
                    private final double mass;
                    Planet() { this(0); }
                    double gravity() { return 9.8; }
                }
                enum Empty { ; int x; }
                """);
        final FilePart.TypeDeclaration planet = outline.types().getFirst();
        assertEquals(List.of("EnumConstant", "EnumConstant", "EnumConstant", "Field", "Constructor", "Method"),
                planet.parts().items().stream().map(part -> part.getClass().getSimpleName()).toList());
        assertEquals(List.of("@Deprecated MERCURY (3.3e23) -", "VENUS (4.8e24) { double gravity() { return 8.9; } }",
                        "EARTH - -"),
                planet.parts().of(BodyPart.EnumConstant.class).stream()
                        .map(constant -> constant.annotations().annotations().stream()
                                .map(tag -> "@" + tag.name().text() + " ").reduce("", String::concat)
                                + constant.name().text()
                                + " " + constant.arguments().map(outline::text).orElse("-")
                                + " " + constant.body().map(outline::text).orElse("-"))
                        .toList());
        assertTrue(planet.parts().of(BodyPart.EnumConstant.class).getFirst().doc().isPresent());
        assertEquals("EARTH,\n    ;", outline.text(planet.parts().items().get(2).range()));
        assertEquals(List.of("Field"), outline.types().getLast().parts().items().stream()
                .map(part -> part.getClass().getSimpleName()).toList());
        assertEquals(List.of(), diagnostics(outline));
    }

    @Test
    void anInterfaceDeclaresConstantsAndMethodsAndAnAnnotationInterfaceItsElements() {
        final Outline outline = outline("""
                interface Shape {
                    double PI = 3.14;
                    double area();
                    default String name() { return "shape"; }
                    static Shape unit() { return null; }
                    private int helper() { return 0; }
                }
                @interface Range {
                    int min() default 0;
                    int[] steps() default {1, 2};
                    String NAME = "range";
                }
                """);
        assertEquals(List.of("Field PI", "Method area -", "Method name { return \"shape\"; }",
                        "Method unit { return null; }", "Method helper { return 0; }"),
                outline.types().getFirst().parts().items().stream().map(part -> switch (part) {
                    case BodyPart.Field field -> "Field " + field.declarators().items().getFirst().name().text();
                    case BodyPart.Method method -> "Method " + method.name().text() + " "
                            + method.body().map(outline::text).orElse("-");
                    default -> part.toString();
                }).toList());
        assertEquals(List.of("min 0", "steps {1, 2}"), outline.types().getLast().parts().of(BodyPart.Method.class)
                .stream().map(element -> element.name().text() + " "
                        + element.defaultValue().map(outline::text).orElse("-"))
                .toList());
        assertEquals(List.of(), diagnostics(outline));
    }

    @Test
    void eachPartIsLinkedToTheKindOfObjectTheTaxonomyHasForIt() {
        final Outline outline = outline("""
                class A {
                    int f; static int s;
                    void m() {} static void sm() {} native void n(); static native void sn();
                    A() {} static {} {}
                    class Inner {} static class Nested {} interface I {} enum E {} record R() {}
                    ;
                }
                abstract class B { abstract void m(); }
                interface I {
                    int X = 1;
                    void a(); default void d() {} static void s() {} private void p() {} private static void ps() {}
                    class C {}
                }
                enum E { A, B { }; E() {} }
                record R(int x) { R {} static int s; }
                @interface N { int v() default 1; int X = 1; }
                """);
        assertEquals(List.of(
                        "Field StaticField Method$Implementation StaticMethod$Implementation"
                                + " Method$NativeImplementation StaticMethod$NativeImplementation Constructor"
                                + " - - MemberClass StaticMemberClass StaticMemberClass"
                                + " StaticMemberClass StaticMemberClass -",
                        "Method$Declaration",
                        "StaticField Method$Declaration Method$DefaultImplementation StaticMethod$Implementation"
                                + " Method$Implementation StaticMethod$Implementation StaticMemberClass",
                        "EnumConstant EnumConstant Constructor",
                        "Constructor StaticField",
                        "Method$Declaration StaticField"),
                outline.types().stream().map(type -> type.parts().items().stream()
                        .map(part -> BodyPartKinds.INSTANCE.of(type.header().keyword(), part)
                                .map(kind -> kind.getClass().getName().substring(kind.getClass().getName()
                                        .lastIndexOf('.') + 1))
                                .orElse("-"))
                        .collect(Collectors.joining(" "))).toList());
    }

    @Test
    void whatTheGrammarAsksOfAMemberIsSaidWhereItIsNotDone() {
        assertEquals(List.of("Error an interface has no constructors: I"),
                diagnostics(outline("interface I { I() {} }")));
        assertEquals(List.of("Error initializers not allowed in interfaces: {"),
                diagnostics(outline("interface I { static {} }")));
        assertEquals(List.of("Error instance initializers not allowed in records: {"),
                diagnostics(outline("record R() { {} }")));
        assertEquals(List.of("Error field declaration must be static (consider replacing field with record"
                + " component): x"), diagnostics(outline("record R() { int x; }")));
        assertEquals(List.of("Error only a record has a compact constructor: A"),
                diagnostics(outline("class A { A {} }")));
        assertEquals(List.of("Error invalid method declaration; return type required: B"),
                diagnostics(outline("class A { B() {} }")));
        assertEquals(List.of("Error missing method body, or declare abstract: void m()"),
                diagnostics(outline("class A { void m(); }")));
        assertEquals(List.of("Error abstract methods cannot have a body: {}"),
                diagnostics(outline("abstract class A { abstract void m() {} }")));
        assertEquals(List.of("Error native methods cannot have a body: {}"),
                diagnostics(outline("class A { native void m() {} }")));
        assertEquals(List.of("Error interface abstract methods cannot have body: {}"),
                diagnostics(outline("interface I { void m() {} }")));
        assertEquals(List.of("Error missing method body, or declare abstract: default void n()"),
                diagnostics(outline("interface I { default void n(); }")));
        assertEquals(List.of("Error illegal combination of modifiers: public and private: private public int x;"),
                diagnostics(outline("class A { private public int x; }")));
        assertEquals(List.of("Error illegal combination of modifiers: final and volatile: final volatile int x;"),
                diagnostics(outline("class A { final volatile int x; }")));
        assertEquals(List.of("Error illegal combination of modifiers: abstract and static: abstract static void m()"),
                diagnostics(outline("abstract class A { abstract static void m(); }")));
        assertEquals(List.of("Error illegal combination of modifiers: static and default: default static void m()"),
                diagnostics(outline("interface I { default static void m() {} }")));
        assertEquals(List.of("Error illegal combination of modifiers: abstract and final: final abstract class B"),
                diagnostics(outline("class A { final abstract class B {} }")));
        assertEquals(List.of("Error modifier private not allowed here: private"),
                diagnostics(outline("interface I { private int x = 1; }")));
        assertEquals(List.of("Error modifier private not allowed here: private"),
                diagnostics(outline("interface I { private class C {} }")));
        assertEquals(List.of("Error modifier final not allowed here: final"),
                diagnostics(outline("interface I { final void m(); }")));
        assertEquals(List.of("Error modifier default not allowed here: default"),
                diagnostics(outline("class A { default void m() {} }")));
        assertEquals(List.of("Error modifier public not allowed here: public"),
                diagnostics(outline("enum E { A; public E() {} }")));
        assertEquals(List.of("Error modifier static not allowed here: static"),
                diagnostics(outline("class A { static A() {} }")));
        assertEquals(List.of("Error repeated modifier: public"),
                diagnostics(outline("class A { public public int x; }")));
        assertEquals(List.of("Error repeated modifier: static"),
                diagnostics(outline("class A { static static {} }")));
        assertEquals(List.of("Error illegal combination of modifiers: abstract and strictfp:"
                                + " abstract strictfp void m()",
                        "Warning strictfp is not required: since Java 17 all floating-point arithmetic is strict:"
                                + " strictfp"),
                diagnostics(outline("abstract class A { abstract strictfp void m(); }")));
        assertEquals(List.of("Error default value only allowed in an annotation interface declaration: int m()"),
                diagnostics(outline("class A { int m() default 1; }")));
        assertEquals(List.of("Error elements in annotation interface declarations cannot declare formal parameters:"
                + " int x"), diagnostics(outline("@interface N { int m(int x); }")));
        assertEquals(List.of("Error throws clause not allowed in @interface members: Exception"),
                diagnostics(outline("@interface N { int m() throws Exception; }")));
        assertEquals(List.of("Error elements in annotation interface declarations cannot be generic methods: T"),
                diagnostics(outline("@interface N { <T> int m(); }")));
        assertEquals(List.of("Error varargs parameter must be the last parameter: int... a"),
                diagnostics(outline("class A { void m(int... a, int b) {} }")));
        assertEquals(List.of("Error 'this' names the receiver parameter only, which comes first: this"),
                diagnostics(outline("class A { void m(int x, A this) {} }")));
        assertEquals(List.of("Error 'var' is not allowed here: var"), diagnostics(outline("class A { var x = 1; }")));
        assertEquals(List.of("Error '(' expected: ;"), diagnostics(outline("class A { <T> int x; }")));
        assertEquals(List.of("Error = expected: ;"), diagnostics(outline("interface I { int x; }")));
        assertEquals(List.of("Error illegal start of expression: ;"), diagnostics(outline("class A { int x = ; }")));
        assertEquals(List.of("Error illegal start of type: {"), diagnostics(outline("class A { public {} }")));
        assertEquals(List.of("Error <identifier> expected: ;"), diagnostics(outline("class A { int; }")));
        assertEquals(List.of("Error ';' expected: "), diagnostics(outline("class A { int x }")));
        assertEquals(List.of("Error '{' or ';' expected: "), diagnostics(outline("class A { void m() }")));
        assertEquals(List.of("Error ',', '}', or ';' expected: B"), diagnostics(outline("enum E { A B }")));
        assertEquals(List.of("Error enum constant expected here: int"), diagnostics(outline("enum E { int x; }")));
        assertEquals(List.of("Error enum constant not expected here: B"),
                diagnostics(outline("enum E { A; B, C; }")));
    }

    @Test
    void whatCannotBeReadInABodyIsSteppedOverToTheNextPart() {
        final Outline outline = outline("class A { 42 int x; public; void m() int y; int z; }");
        assertEquals(List.of("Unrecognised 42 int x;", "Unrecognised public;", "Method void m() int y;",
                "Field int z;"), parts(outline, outline.types().getFirst()));
        assertEquals(List.of("Error illegal start of type: 42", "Error illegal start of type: ;",
                "Error '{' or ';' expected: int"), diagnostics(outline));
    }

    @Test
    void parenthesesHoldASemicolonOnlyInBracesBetweenThem() {
        final Outline outline = outline("enum E { A(() -> { int x = 1; }), B(new Object() { int y; }); }");
        assertEquals(List.of("(() -> { int x = 1; })", "(new Object() { int y; })"), outline.types().getFirst()
                .parts().of(BodyPart.EnumConstant.class).stream()
                .map(constant -> outline.text(constant.arguments().orElseThrow()))
                .toList());
        assertEquals(List.of(), diagnostics(outline));
        assertEquals(List.of("Error ')' expected: ("), diagnostics(outline("enum E { A(1; }")));
    }

    @Test
    void aDocumentationCommentInABodyIsSaidToBeAttachedToNoneWhereJavacSaysSo() {
        assertEquals(List.of("Warning documentation comment is not attached to any declaration: /** a */"),
                diagnostics(outline("class A { /** a */ /** b */ int x; }")));
        assertEquals(List.of("Warning documentation comment is not attached to any declaration: /** a */"),
                diagnostics(outline("class A { /** a */ ; int x; }")));
        assertEquals(List.of("Warning documentation comment is not attached to any declaration: /** d */"),
                diagnostics(outline("class A { @Deprecated /** d */ void m() {} }")));
        assertEquals(List.of("Warning documentation comment is not attached to any declaration: /** d */"),
                diagnostics(outline("class A { int x; /** d */ } class B {}")));
        assertEquals(List.of(), diagnostics(outline("class A { int x; /** d */ }")));
        assertEquals(List.of(), diagnostics(outline("class A { /** a */ /** b */ static {} } class B {}")));
        assertEquals(List.of(), diagnostics(outline("class A { void m() { /** inner */ } int y; }")));
        assertEquals(List.of(), diagnostics(outline("enum E { /** a */ A, /** b */ B } class C {}")));

        final Outline initialized = outline("class A { /** Loads. */ static { } }");
        assertEquals(Optional.of("/** Loads. */ static { }"), initialized.types().getFirst().parts()
                .of(BodyPart.Initializer.class).stream().findFirst().map(part -> initialized.text(part.range())));
    }

    /** The parts of the body are in order, and each runs to the next, from the first one up to the closing brace. */
    private void assertCovered(final Outline outline, final FilePart.TypeDeclaration type) {
        final List<BodyPart> parts = type.parts().items();
        int end = parts.getFirst().range().from();
        for (final BodyPart part : parts) {
            assertEquals(end, part.range().from(), part.toString());
            assertTrue(part.range().to() > part.range().from(), part.toString());
            end = part.range().to();
        }
        assertEquals(type.body().to() - 1, end);
        assertEquals("}", outline.text(new TokenRange(end, end + 1)));
    }

    private Outline outline(final String text) {
        return JavaOutliner.INSTANCE.outline(JavaLexer.INSTANCE.scan(new SourceText("", text)));
    }

    /** Each part as its kind and its text. */
    private List<String> parts(final Outline outline, final FilePart.TypeDeclaration type) {
        return type.parts().items().stream()
                .map(part -> part.getClass().getSimpleName() + " " + outline.text(part.range()))
                .toList();
    }

    /** Each formal parameter as written, with what it is read as between its parts. */
    private List<String> formal(final Outline outline, final Parameters parameters) {
        return parameters.formal().stream()
                .map(parameter -> parameter.modifiers().items().stream()
                        .map(item -> outline.text(item.range()) + " ").reduce("", String::concat)
                        + outline.text(parameter.type().range())
                        + (parameter.variable() ? ", any number of, " : " ") + parameter.name().text()
                        + (parameter.dimensions().isEmpty() ? "" : " " + outline.text(parameter.dimensions())))
                .toList();
    }

    private List<String> words(final Modifiers modifiers) {
        return modifiers.words().stream().map(ModifierItem.Word::word).toList();
    }

    private List<String> texts(final Outline outline, final List<TypeText> types) {
        return types.stream().map(type -> outline.text(type.range())).toList();
    }

    /** Each diagnostic as its severity, its message and the text it is about. */
    private List<String> diagnostics(final Outline outline) {
        final String text = outline.tokens().source().text();
        return outline.diagnostics().items().stream()
                .map(diagnostic -> diagnostic.severity().getClass().getSimpleName() + " " + diagnostic.message()
                        + ": " + text.substring(diagnostic.start(), diagnostic.end()))
                .toList();
    }
}
