package com.homing_labs.cq.lang.java.lexer;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JavaLexerTest {

    @Test
    void aWordIsAKeywordAnIdentifierOrALiteral() {
        assertEquals(List.of("Keyword class", "Identifier record", "Identifier sealed", "Identifier permits",
                        "Keyword _", "Identifier __", "Identifier $x", "Identifier été", "BooleanLiteral true",
                        "BooleanLiteral false", "NullLiteral null", "Keyword goto"),
                lexed("class record sealed permits _ __ $x été true false null goto"));
    }

    @Test
    void aNumberIsReadWithItsPrefixPointExponentAndSuffix() {
        assertEquals(List.of("IntegerLiteral 0", "IntegerLiteral 7L", "IntegerLiteral 0x1F", "IntegerLiteral 0b1010",
                        "IntegerLiteral 0777", "IntegerLiteral 1_000", "IntegerLiteral 0x7fff_ffffL",
                        "FloatingPointLiteral 3.14", "FloatingPointLiteral .5", "FloatingPointLiteral 1e10",
                        "FloatingPointLiteral 1E-5f", "FloatingPointLiteral 0x1.8p1", "FloatingPointLiteral 2d",
                        "FloatingPointLiteral 1.f", "FloatingPointLiteral 0x1p3f", "FloatingPointLiteral 5.",
                        "IntegerLiteral 0xFD"),
                lexed("0 7L 0x1F 0b1010 0777 1_000 0x7fff_ffffL 3.14 .5 1e10 1E-5f 0x1.8p1 2d 1.f 0x1p3f 5. 0xFD"));
    }

    @Test
    void aMalformedNumberIsStillOneToken() {
        assertEquals(List.of("hexadecimal numbers must contain at least one hexadecimal digit 0..2",
                        "illegal underscore 3..5", "binary numbers must contain at least one binary digit 6..8",
                        "malformed floating-point literal 9..11", "malformed floating-point literal 12..17",
                        "malformed number 18..23", "illegal underscore 26..28",
                        "malformed floating-point literal 29..33"),
                diagnostics("0x 1_ 0b 1e 0x1.8 12abc 1._5 1.0L"));
        assertEquals(List.of("IntegerLiteral 0x", "IntegerLiteral 1_", "IntegerLiteral 0b",
                        "FloatingPointLiteral 1e", "FloatingPointLiteral 0x1.8", "IntegerLiteral 12abc",
                        "FloatingPointLiteral 1._5", "FloatingPointLiteral 1.0L"),
                lexed("0x 1_ 0b 1e 0x1.8 12abc 1._5 1.0L"));
    }

    @Test
    void stringsCharactersAndTextBlocksAreSingleTokensWhateverTheyHold() {
        assertEquals(List.of("StringLiteral \"a\\\"{b\"", "CharacterLiteral 'c'", "CharacterLiteral '\\n'",
                        "CharacterLiteral '\\177'", "CharacterLiteral '\\''",
                        "TextBlock \"\"\"\n  a \"quoted\" \\\"\"\" }\n  \"\"\"", "Identifier end"),
                lexed("\"a\\\"{b\" 'c' '\\n' '\\177' '\\'' \"\"\"\n  a \"quoted\" \\\"\"\" }\n  \"\"\" end"));
        assertEquals(List.of(), diagnostics("\"a\\s\\0\\377\" \"\"\"\n  joined \\\n  line\"\"\""));
    }

    @Test
    void aLiteralLeftOpenEndsWhereItsLineDoes() {
        assertEquals(List.of("StringLiteral \"abc", "Identifier next", "CharacterLiteral '", "Identifier x",
                        "CharacterLiteral ''", "CharacterLiteral 'a", "Identifier b", "CharacterLiteral '"),
                lexed("\"abc\nnext '\nx '' 'ab'"));
        assertEquals(List.of("unclosed string literal 0..1", "unclosed character literal 10..11",
                        "empty character literal 14..16", "unclosed character literal 17..18",
                        "unclosed character literal 20..21"),
                diagnostics("\"abc\nnext '\nx '' 'ab'"));
        assertEquals(List.of("illegal text block open delimiter sequence, missing line terminator 0..4",
                        "unclosed text block 0..3"),
                diagnostics("\"\"\" x"));
        assertEquals(List.of("illegal escape character 1..3"), diagnostics("\"\\q\""));
    }

    @Test
    void commentsAreTokensOfTheirOwnKinds() {
        assertEquals(List.of("LineComment // line", "BlockComment /* block */", "DocComment /** doc */",
                        "BlockComment /**/", "DocComment /***/", "Identifier x", "LineComment //"),
                lexed("// line\n/* block */ /** doc */ /**/ /***/ x //"));
        assertEquals(List.of("BlockComment /* never closed"), lexed("/* never closed"));
        assertEquals(List.of("unclosed comment 0..2"), diagnostics("/* never closed"));
    }

    @Test
    void anOperatorIsAsLongAsItCanBeButAGreaterThanAlwaysStandsAlone() {
        assertEquals(List.of("Identifier a", "Operator >", "Operator >", "Operator =", "Identifier b",
                        "Operator >", "Operator =", "Operator <<=", "Operator ->", "Operator ++", "Operator !=",
                        "Operator &&", "Operator ?", "Operator :"),
                lexed("a>>=b >= <<= -> ++ != && ? :"));
        assertEquals(List.of("Identifier List", "Operator <", "Identifier List", "Operator <", "Identifier String",
                        "Operator >", "Operator >"),
                lexed("List<List<String>>"));
    }

    @Test
    void separatorsIncludeTheEllipsisTheDoubleColonAndTheAt() {
        assertEquals(List.of("Separator @", "Keyword interface", "Identifier String", "Separator ...",
                        "Identifier Foo", "Separator ::", "Keyword new", "Separator .", "Separator .",
                        "Separator (", "Separator )", "Separator {", "Separator }", "Separator [", "Separator ]",
                        "Separator ;", "Separator ,"),
                lexed("@interface String... Foo::new .. (){}[];,"));
    }

    @Test
    void nonSealedIsThreeTokens() {
        assertEquals(List.of("Identifier non", "Operator -", "Identifier sealed", "Keyword class"),
                lexed("non-sealed class"));
    }

    @Test
    void aCharacterNoTokenStartsWithIsAnErrorTokenAndTheLexerGoesOn() {
        assertEquals(List.of("Identifier a", "Error #", "Identifier b", "Error `"), lexed("a#b`"));
        assertEquals(List.of("illegal character: '\\u0023' 1..2", "illegal character: '\\u0060' 3..4"),
                diagnostics("a#b`"));
    }

    @Test
    void unicodeEscapesAreTranslatedButOffsetsStayTheTextsOwn() {
        final TokenStream stream = JavaLexer.INSTANCE.scan(new SourceText("class \\u0041 {}"));
        assertEquals(new Token(TokenKind.Identifier.INSTANCE, 6, 12), stream.get(1));
        assertEquals("A", stream.text(stream.get(1)));

        assertEquals(List.of("StringLiteral \"a\\u0022", "Identifier b"), lexed("\"a\\u0022 b"));
        assertEquals(List.of("StringLiteral \"\\\\u0041\""), lexed("\"\\\\u0041\""));
        assertEquals(List.of("Identifier x", "LineComment //", "Identifier y"), lexed("x//\\u000ay"));
        assertEquals(List.of("Identifier a\\uuu0062"), lexed("a\\uuu0062"));
        assertEquals("ab", JavaLexer.INSTANCE.scan(new SourceText("a\\uuu0062")).text(new Token(
                TokenKind.Identifier.INSTANCE, 0, 9)));
        assertEquals(List.of("illegal unicode escape 1..7","illegal character: '\\u005c' 1..2"),
                diagnostics("a\\u00G1"));
    }

    @Test
    void theSubstituteCharacterMayEndTheText() {
        assertEquals(List.of("Identifier x"), lexed("x\u001a"));
        assertEquals(List.of(), diagnostics("x\u001a"));
    }

    @Test
    void aPositionCountsLinesEndedAnyWay() {
        final SourceText text = new SourceText("ab\ncd\r\nef\rg");
        assertEquals(new Position(1, 1), text.position(0));
        assertEquals(new Position(1, 3), text.position(2));
        assertEquals(new Position(2, 2), text.position(4));
        assertEquals(new Position(3, 1), text.position(7));
        assertEquals(new Position(4, 1), text.position(10));
    }

    /** Each token as its kind and its text, in the text as written. */
    private List<String> lexed(final String text) {
        final TokenStream stream = JavaLexer.INSTANCE.scan(new SourceText(text));
        return stream.tokens().items().stream()
                .map(token -> token.kind().getClass().getSimpleName() + " " + text.substring(token.start(),
                        token.end()))
                .toList();
    }

    /** Each diagnostic as its message and where it lies. */
    private List<String> diagnostics(final String text) {
        return JavaLexer.INSTANCE.scan(new SourceText(text)).diagnostics().items().stream()
                .map(diagnostic -> diagnostic.message() + " " + diagnostic.start() + ".." + diagnostic.end())
                .toList();
    }
}
