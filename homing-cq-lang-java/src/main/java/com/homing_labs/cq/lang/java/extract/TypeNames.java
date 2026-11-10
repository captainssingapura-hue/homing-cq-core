package com.homing_labs.cq.lang.java.extract;

import com.homing_labs.cq.lang.java.lexer.Token;
import com.homing_labs.cq.lang.java.lexer.TokenKind;
import com.homing_labs.cq.lang.java.lexer.TokenStream;
import com.homing_labs.cq.lang.java.model.ClassName;
import com.homing_labs.cq.lang.java.model.Parameters;
import com.homing_labs.cq.lang.java.model.PrimitiveType;
import com.homing_labs.cq.lang.java.model.TypeUse;
import com.homing_labs.cq.lang.java.outline.Parameter;
import com.homing_labs.cq.lang.java.outline.TokenRange;
import hue.captains.singapura.tao.ontology.StatelessFunctionalObject;

import java.util.List;

/**
 * Types by the names a source writes them with, and by the names a class file has for them: a type as written, erased
 * to its names and its dimensions; a class file's type, by its qualified name; and whether the one can be the other.
 */
record TypeNames() implements StatelessFunctionalObject {

    static final TypeNames INSTANCE = new TypeNames();

    /**
     * A type as written, erased to the names it is written with and its dimensions: {@code java.util.Map.Entry[]} for
     * {@code java.util.Map.@A Entry<K, V>[]}. A type variable stays as it is named.
     */
    String erased(final Lexed lexed, final TokenRange type, final int dimensions) {
        final TokenStream tokens = lexed.tokens();
        final StringBuilder name = new StringBuilder();
        int brackets = dimensions;
        int depth = 0;
        for (int at = type.from(); at < type.to(); at++) {
            final Token token = tokens.get(at);
            if (token.kind().comment()) {
                continue;
            }
            final String text = tokens.text(token);
            if (text.equals("<")) {
                depth++;
            } else if (text.equals(">")) {
                depth--;
            } else if (depth > 0) {
                continue;
            } else if (text.equals("@")) {
                at = pastAnnotation(lexed, at, type.to()) - 1;
            } else if (text.equals("[")) {
                brackets++;
            } else if (text.equals(".") || token.kind() instanceof TokenKind.Identifier
                    || token.kind() instanceof TokenKind.Keyword) {
                name.append(text);
            }
        }
        return name + "[]".repeat(brackets);
    }

    /** The types of formal parameters as written, each erased to its names and its dimensions. */
    List<String> writtenTypes(final Lexed lexed, final List<Parameter.Formal> parameters) {
        return parameters.stream()
                .map(parameter -> erased(lexed, parameter.type().range(),
                        brackets(lexed, parameter.dimensions()) + (parameter.variable() ? 1 : 0)))
                .toList();
    }

    List<String> qualified(final Parameters parameters) {
        return parameters.items().stream().map(this::qualified).toList();
    }

    /** A type of the class file by its qualified name and its dimensions: {@code java.util.Map.Entry[]}. */
    String qualified(final TypeUse type) {
        return switch (type) {
            case TypeUse.Primitive primitive -> keyword(primitive.type());
            case TypeUse.Named named -> qualified(named.name());
            case TypeUse.Parameterized parameterized -> qualified(parameterized.name());
            case TypeUse.Variable variable -> variable.name();
            case TypeUse.Array array -> qualified(array.component()) + "[]";
        };
    }

    /** A class by its qualified name, as a source writes it: {@code java.util.Map.Entry} for {@code Map$Entry}. */
    String qualified(final ClassName name) {
        return name.binaryName().replace('$', '.');
    }

    /**
     * Whether a type as written can be the type the class file names: the same, or written by fewer names, from the
     * right - {@code Map.Entry[]} or {@code Entry[]} for {@code java.util.Map.Entry[]}.
     */
    boolean fits(final String written, final String compiled) {
        return written.equals(compiled) || compiled.endsWith("." + written);
    }

    /** Whether two types have the same simple name and dimensions: {@code Entry[]} for {@code a.B.Entry[]}. */
    boolean sameSimpleName(final String written, final String compiled) {
        return simpleName(written).equals(simpleName(compiled));
    }

    private String simpleName(final String type) {
        final String name = type.replace("[]", "");
        return name.substring(name.lastIndexOf('.') + 1) + "[]".repeat((type.length() - name.length()) / 2);
    }

    private String keyword(final PrimitiveType type) {
        return switch (type) {
            case PrimitiveType.Boolean _ -> "boolean";
            case PrimitiveType.Byte _ -> "byte";
            case PrimitiveType.Char _ -> "char";
            case PrimitiveType.Short _ -> "short";
            case PrimitiveType.Int _ -> "int";
            case PrimitiveType.Long _ -> "long";
            case PrimitiveType.Float _ -> "float";
            case PrimitiveType.Double _ -> "double";
        };
    }

    /** The index past an annotation written in a type, from its {@code @}: past its name, and its arguments. */
    private int pastAnnotation(final Lexed lexed, final int at, final int end) {
        final TokenStream tokens = lexed.tokens();
        int past = lexed.following(at, end);
        while (past < end && tokens.get(past).kind() instanceof TokenKind.Identifier) {
            past = lexed.following(past, end);
            if (past < end && tokens.text(tokens.get(past)).equals(".")) {
                past = lexed.following(past, end);
            } else {
                break;
            }
        }
        if (past < end && tokens.text(tokens.get(past)).equals("(")) {
            int depth = 0;
            for (; past < end; past++) {
                final String text = tokens.text(tokens.get(past));
                if (text.equals("(")) {
                    depth++;
                } else if (text.equals(")") && --depth == 0) {
                    return past + 1;
                }
            }
        }
        return past;
    }

    private int brackets(final Lexed lexed, final TokenRange range) {
        int brackets = 0;
        for (int at = range.from(); at < range.to(); at++) {
            if (lexed.tokens().text(lexed.tokens().get(at)).equals("[")) {
                brackets++;
            }
        }
        return brackets;
    }
}
