package com.homing_labs.cq.lang.java.outline;

import com.homing_labs.cq.lang.java.lexer.Diagnostic;
import com.homing_labs.cq.lang.java.lexer.Diagnostics;
import com.homing_labs.cq.lang.java.lexer.TokenStream;
import hue.captains.singapura.tao.ontology.Mutable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * One reading of a file's top level: the phase the file is in - before its imports, among them, or among its types -
 * and the parts found so far. The body of each type is read by a {@link BodyScan} of its own.
 */
final class FileScan implements Mutable {

    private static final int BEFORE_IMPORTS = 0;

    private static final int AMONG_IMPORTS = 1;

    private static final int AMONG_TYPES = 2;

    /** The file's tokens, as this reads them. */
    private final TokenScan scan;

    private final String fileName;

    /** Where each part found starts, and how to make it once its range is known. */
    private final List<Integer> starts = new ArrayList<>();

    private final List<Function<TokenRange, FilePart>> parts = new ArrayList<>();

    /** Semicolons that declare nothing, with no type after them yet: an import after one is an error. */
    private final List<Integer> semicolons = new ArrayList<>();

    private int phase = BEFORE_IMPORTS;

    private boolean packaged;

    FileScan(final TokenStream stream) {
        this.scan = new TokenScan(stream);
        this.fileName = stream.source().fileName();
    }

    Outline run() {
        while (scan.at() < scan.size()) {
            final int start = scan.at();
            final List<ModifierItem> modifiers = scan.modifiers();
            final Optional<TypeKeyword> keyword = TypeHeaders.INSTANCE.keyword(scan);
            if (scan.is("package")) {
                packageDeclaration(start, modifiers);
            } else if (scan.is("import")) {
                importDeclaration(start, modifiers);
            } else if (scan.is(";") && modifiers.isEmpty()) {
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
        scan.docs.dangling(scan.stream, scan.unread).forEach(token -> scan.warning("documentation comment is not"
                + " attached to any declaration", token, token + 1));
        scan.diagnostics.sort(Comparator.comparingInt(Diagnostic::start));
        return new Outline(scan.stream, new FileParts(parts()), new Diagnostics(scan.diagnostics));
    }

    private void packageDeclaration(final int start, final List<ModifierItem> modifiers) {
        final boolean packageInfo = fileName.equals("package-info.java");
        final Optional<DocComment> doc = packageInfo ? scan.attach(start) : Optional.empty();
        final Modifiers written = new Modifiers(modifiers);
        written.words().forEach(word -> scan.error("modifier " + word.word() + " not allowed here", word.range()));
        if (!packageInfo && !written.annotations().isEmpty()) {
            scan.error("package annotations should be in file package-info.java",
                    written.annotations().getFirst().range().from(), written.annotations().getLast().range().to());
        }
        if (packaged) {
            scan.error("a file declares one package only", scan.at(), scan.at() + 1);
        } else if (phase != BEFORE_IMPORTS) {
            scan.error("the package declaration comes before the imports and the types", scan.at(), scan.at() + 1);
        }
        packaged = true;
        scan.advance();
        final Name name = scan.name();
        semicolon();
        phase = Math.max(phase, AMONG_IMPORTS);
        found(doc.map(DocComment::token).orElse(start),
                range -> new FilePart.PackageDeclaration(doc, written, name, range));
    }

    private void importDeclaration(final int start, final List<ModifierItem> modifiers) {
        if (!modifiers.isEmpty()) {
            scan.error("an import takes no modifiers and no annotations", start, scan.at());
        }
        if (phase == AMONG_TYPES) {
            scan.error("the imports come before the types", scan.at(), scan.at() + 1);
        }
        semicolons.forEach(semicolon -> scan.error("extraneous semicolon", semicolon, semicolon + 1));
        semicolons.clear();
        scan.advance();
        final boolean statically = scan.is("static");
        final boolean module = !statically && scan.is("module") && scan.identifier(scan.at())
                && scan.identifier(scan.next(scan.at()));
        if (statically || module) {
            scan.advance();
        }
        final Name name = scan.name();
        final boolean onDemand = !module && scan.is(".") && scan.is(scan.next(scan.at()), "*");
        if (onDemand) {
            scan.advance();
            scan.advance();
        } else if (!module && !name.text().isEmpty() && !name.text().contains(".")) {
            scan.error("'.' expected", name.range());
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
        scan.advance();
        if (phase != AMONG_TYPES) {
            semicolons.add(start);
        }
        found(start, FilePart.EmptyDeclaration::new);
    }

    private void typeDeclaration(final int start, final List<ModifierItem> modifiers, final TypeKeyword keyword) {
        final TypeHeader header = TypeHeaders.INSTANCE.read(scan, start, modifiers, keyword);
        final TypeBody body = TypeHeaders.INSTANCE.body(scan, this::startsPart);
        if (body.end() instanceof BodyEnd.Unterminated) {
            scan.error("reached end of file while parsing: the body of " + header.name().text() + " is never closed",
                    body.range().from(), body.range().from() + 1);
        }
        final BodyParts read = new BodyParts(body.end() instanceof BodyEnd.Missing ? List.of()
                : new BodyScan(scan, header, body.inside()).run());
        HeaderChecks.INSTANCE.checkHeader(scan, header, Optional.empty());
        final String name = header.name().text();
        if (header.modifiers().has("public") && fileName.endsWith(".java") && !name.isEmpty()
                && !fileName.equals(name + ".java")) {
            scan.error(keyword.word() + " " + name + " is public, should be declared in a file named " + name + ".java",
                    header.name().range());
        }
        phase = AMONG_TYPES;
        semicolons.clear();
        found(header.doc().map(DocComment::token).orElse(start), range -> new FilePart.TypeDeclaration(header,
                body.range(), body.end(), read, range));
    }

    private boolean moduleDeclaration() {
        return scan.is("module") && scan.identifier(scan.at()) && scan.identifier(scan.next(scan.at()))
                || scan.is("open") && scan.is(scan.next(scan.at()), "module");
    }

    /** Whether what follows is shaped like a method or a field: a type, a name, and what follows a member's name. */
    private boolean memberShaped() {
        int index = scan.is("<") ? pastAngles(scan.at()) : scan.at();
        if (!(scan.identifier(index) || scan.is(index, "void")
                || scan.keyword(index) && scan.primitive(scan.text(index)))) {
            return false;
        }
        index = scan.next(index);
        while (index < scan.size()) {
            if (scan.is(index, ".") || scan.is(index, "[") || scan.is(index, "]")) {
                index = scan.next(index);
            } else if (scan.is(index, "<")) {
                index = pastAngles(index);
            } else if (scan.identifier(index)) {
                final int after = scan.next(index);
                if (scan.is(after, "(") || scan.is(after, "=") || scan.is(after, ";") || scan.is(after, ",")) {
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
        while (index < scan.size() && !scan.is(index, "{") && !scan.is(index, ";")) {
            if (scan.is(index, "<")) {
                depth++;
            } else if (scan.is(index, ">") && --depth == 0) {
                return scan.next(index);
            }
            index = scan.next(index);
        }
        return index;
    }

    /** A form of file that is not read yet: the rest of it, with its documentation comment, is one part, unread. */
    private void notHandled(final int start, final String message) {
        final int from = scan.attach(start).map(DocComment::token).orElse(start);
        scan.error(message, scan.at(), Math.min(scan.at() + 1, scan.size()));
        scan.unread.add(new TokenRange(from, scan.size()));
        scan.toEnd();
        found(from, FilePart.Unrecognised::new);
    }

    private void unrecognised(final int start) {
        if (scan.at() == start) {
            scan.advance();
        }
        recover();
        scan.error("class, interface, enum, or record expected", start, scan.last() + 1);
        found(start, FilePart.Unrecognised::new);
    }

    private void semicolon() {
        if (scan.is(";")) {
            scan.advance();
            return;
        }
        scan.error("';' expected", scan.at(), Math.min(scan.at() + 1, scan.size()));
        recover();
    }

    /** Steps over what cannot be read: up to the next semicolon and over it, or up to the next start of a part. */
    private void recover() {
        while (scan.at() < scan.size() && !startsPart(scan.at())) {
            if (scan.is("{")) {
                scan.skipBlock();
            } else if (scan.is(";")) {
                scan.advance();
                return;
            } else {
                scan.advance();
            }
        }
    }

    private boolean startsPart(final int index) {
        return scan.is(index, "package") || scan.is(index, "import") || scan.is(index, "@") || scan.is(index, "class")
                || scan.is(index, "interface") || scan.is(index, "enum")
                || scan.is(index, "record") && scan.identifier(scan.next(index))
                || scan.keyword(index) && scan.modifierWord(scan.text(index));
    }

    private void found(final int start, final Function<TokenRange, FilePart> part) {
        starts.add(start);
        parts.add(part);
    }

    private List<FilePart> parts() {
        final List<FilePart> made = new ArrayList<>();
        final int first = starts.isEmpty() ? scan.size() : starts.getFirst();
        if (first > 0) {
            made.add(new FilePart.Preamble(new TokenRange(0, first)));
        }
        for (int index = 0; index < starts.size(); index++) {
            final int end = index + 1 < starts.size() ? starts.get(index + 1) : scan.size();
            made.add(parts.get(index).apply(new TokenRange(starts.get(index), end)));
        }
        return made;
    }
}
