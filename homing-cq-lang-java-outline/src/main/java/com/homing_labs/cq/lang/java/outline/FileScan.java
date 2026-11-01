package com.homing_labs.cq.lang.java.outline;

import com.homing_labs.cq.lang.java.lexer.Diagnostic;
import com.homing_labs.cq.lang.java.lexer.Diagnostics;
import com.homing_labs.cq.lang.java.lexer.TokenStream;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * One reading of a file's top level: the phase the file is in - before its imports, among them, or among its types -
 * and the parts found so far. The body of each type is read by a {@link BodyScan} of its own.
 */
final class FileScan extends TokenScan {

    private static final int BEFORE_IMPORTS = 0;

    private static final int AMONG_IMPORTS = 1;

    private static final int AMONG_TYPES = 2;

    private final String fileName;

    /** Where each part found starts, and how to make it once its range is known. */
    private final List<Integer> starts = new ArrayList<>();

    private final List<Function<TokenRange, FilePart>> parts = new ArrayList<>();

    /** Semicolons that declare nothing, with no type after them yet: an import after one is an error. */
    private final List<Integer> semicolons = new ArrayList<>();

    private int phase = BEFORE_IMPORTS;

    private boolean packaged;

    FileScan(final TokenStream stream) {
        super(stream);
        this.fileName = stream.source().fileName();
    }

    Outline run() {
        while (at < size()) {
            final int start = at;
            final List<ModifierItem> modifiers = modifiers();
            final Optional<TypeKeyword> keyword = typeKeyword();
            if (is("package")) {
                packageDeclaration(start, modifiers);
            } else if (is("import")) {
                importDeclaration(start, modifiers);
            } else if (is(";") && modifiers.isEmpty()) {
                emptyDeclaration(start);
            } else if (keyword.isPresent()) {
                typeDeclaration(start, modifiers, keyword.get());
            } else if (moduleDeclaration()) {
                notHandled(start, "a module declaration is not read yet: only ordinary compilation units are");
            } else if (memberShaped()) {
                notHandled(start, "a method or a field outside any class is not read yet: only ordinary compilation"
                        + " units are");
            } else {
                unrecognised(start);
            }
        }
        docs.dangling(stream, unread).forEach(token -> warning("documentation comment is not attached to any"
                + " declaration", token, token + 1));
        diagnostics.sort(Comparator.comparingInt(Diagnostic::start));
        return new Outline(stream, new FileParts(parts()), new Diagnostics(diagnostics));
    }

    private void packageDeclaration(final int start, final List<ModifierItem> modifiers) {
        final boolean packageInfo = fileName.equals("package-info.java");
        final Optional<DocComment> doc = packageInfo ? attach(start) : Optional.empty();
        final Modifiers written = new Modifiers(modifiers);
        written.words().forEach(word -> error("modifier " + word.word() + " not allowed here", word.range()));
        if (!packageInfo && !written.annotations().isEmpty()) {
            error("package annotations should be in file package-info.java",
                    written.annotations().getFirst().range().from(), written.annotations().getLast().range().to());
        }
        if (packaged) {
            error("a file declares one package only", at, at + 1);
        } else if (phase != BEFORE_IMPORTS) {
            error("the package declaration comes before the imports and the types", at, at + 1);
        }
        packaged = true;
        advance();
        final Name name = name();
        semicolon();
        phase = Math.max(phase, AMONG_IMPORTS);
        found(doc.map(DocComment::token).orElse(start),
                range -> new FilePart.PackageDeclaration(doc, written, name, range));
    }

    private void importDeclaration(final int start, final List<ModifierItem> modifiers) {
        if (!modifiers.isEmpty()) {
            error("an import takes no modifiers and no annotations", start, at);
        }
        if (phase == AMONG_TYPES) {
            error("the imports come before the types", at, at + 1);
        }
        semicolons.forEach(semicolon -> error("extraneous semicolon", semicolon, semicolon + 1));
        semicolons.clear();
        advance();
        final boolean statically = is("static");
        final boolean module = !statically && is("module") && identifier(at) && identifier(next(at));
        if (statically || module) {
            advance();
        }
        final Name name = name();
        final boolean onDemand = !module && is(".") && is(next(at), "*");
        if (onDemand) {
            advance();
            advance();
        } else if (!module && !name.text().isEmpty() && !name.text().contains(".")) {
            error("'.' expected", name.range());
        }
        semicolon();
        phase = Math.max(phase, AMONG_IMPORTS);
        final ImportKind kind = module ? ImportKind.Module.INSTANCE
                : statically && onDemand ? ImportKind.StaticOnDemand.INSTANCE
                : statically ? ImportKind.SingleStatic.INSTANCE
                : onDemand ? ImportKind.OnDemand.INSTANCE
                : ImportKind.SingleType.INSTANCE;
        found(start, range -> new FilePart.ImportDeclaration(kind, name, range));
    }

    private void emptyDeclaration(final int start) {
        advance();
        if (phase != AMONG_TYPES) {
            semicolons.add(start);
        }
        found(start, FilePart.EmptyDeclaration::new);
    }

    private void typeDeclaration(final int start, final List<ModifierItem> modifiers, final TypeKeyword keyword) {
        final TypeHeader header = typeHeader(start, modifiers, keyword);
        final Body body = typeBody();
        if (body.end() instanceof BodyEnd.Unterminated) {
            error("reached end of file while parsing: the body of " + header.name().text() + " is never closed",
                    body.range().from(), body.range().from() + 1);
        }
        final BodyParts read = new BodyParts(body.end() instanceof BodyEnd.Missing ? List.of()
                : new BodyScan(this, header, body.inside()).run());
        checkHeader(header, Optional.empty());
        final String name = header.name().text();
        if (header.modifiers().has("public") && fileName.endsWith(".java") && !name.isEmpty()
                && !fileName.equals(name + ".java")) {
            error(keyword.word() + " " + name + " is public, should be declared in a file named " + name + ".java",
                    header.name().range());
        }
        phase = AMONG_TYPES;
        semicolons.clear();
        found(header.doc().map(DocComment::token).orElse(start), range -> new FilePart.TypeDeclaration(header,
                body.range(), body.end(), read, range));
    }

    private boolean moduleDeclaration() {
        return is("module") && identifier(at) && identifier(next(at)) || is("open") && is(next(at), "module");
    }

    /** Whether what follows is shaped like a method or a field: a type, a name, and what follows a member's name. */
    private boolean memberShaped() {
        int index = is("<") ? pastAngles(at) : at;
        if (!(identifier(index) || is(index, "void") || keyword(index) && primitive(text(index)))) {
            return false;
        }
        index = next(index);
        while (index < size()) {
            if (is(index, ".") || is(index, "[") || is(index, "]")) {
                index = next(index);
            } else if (is(index, "<")) {
                index = pastAngles(index);
            } else if (identifier(index)) {
                final int after = next(index);
                if (is(after, "(") || is(after, "=") || is(after, ";") || is(after, ",")) {
                    return true;
                }
                index = after;
            } else {
                return false;
            }
        }
        return false;
    }

    private int pastAngles(final int from) {
        int depth = 0;
        int index = from;
        while (index < size() && !is(index, "{") && !is(index, ";")) {
            if (is(index, "<")) {
                depth++;
            } else if (is(index, ">") && --depth == 0) {
                return next(index);
            }
            index = next(index);
        }
        return index;
    }

    /** A form of file that is not read yet: the rest of it, with its documentation comment, is one part, unread. */
    private void notHandled(final int start, final String message) {
        final int from = attach(start).map(DocComment::token).orElse(start);
        error(message, at, Math.min(at + 1, size()));
        unread.add(new TokenRange(from, size()));
        last = size() - 1;
        at = size();
        found(from, FilePart.Unrecognised::new);
    }

    private void unrecognised(final int start) {
        if (at == start) {
            advance();
        }
        recover();
        error("class, interface, enum, or record expected", start, last + 1);
        found(start, FilePart.Unrecognised::new);
    }

    private void semicolon() {
        if (is(";")) {
            advance();
            return;
        }
        error("';' expected", at, Math.min(at + 1, size()));
        recover();
    }

    /** Steps over what cannot be read: up to the next semicolon and over it, or up to the next start of a part. */
    private void recover() {
        while (at < size() && !startsPart(at)) {
            if (is("{")) {
                skipBlock();
            } else if (is(";")) {
                advance();
                return;
            } else {
                advance();
            }
        }
    }

    @Override
    boolean startsPart(final int index) {
        return is(index, "package") || is(index, "import") || is(index, "@") || is(index, "class")
                || is(index, "interface") || is(index, "enum") || is(index, "record") && identifier(next(index))
                || keyword(index) && modifierWord(text(index));
    }

    private void found(final int start, final Function<TokenRange, FilePart> part) {
        starts.add(start);
        parts.add(part);
    }

    private List<FilePart> parts() {
        final List<FilePart> made = new ArrayList<>();
        final int first = starts.isEmpty() ? size() : starts.getFirst();
        if (first > 0) {
            made.add(new FilePart.Preamble(new TokenRange(0, first)));
        }
        for (int index = 0; index < starts.size(); index++) {
            final int end = index + 1 < starts.size() ? starts.get(index + 1) : size();
            made.add(parts.get(index).apply(new TokenRange(starts.get(index), end)));
        }
        return made;
    }
}
