package com.homing_labs.cq.lang.java.extract;

import com.homing_labs.cq.lang.java.extract.SourceReading.Declared;
import com.homing_labs.cq.lang.java.model.ConstructorSignature;
import com.homing_labs.cq.lang.java.model.ConstructorValue;
import com.homing_labs.cq.lang.java.model.MethodSignature;
import com.homing_labs.cq.lang.java.model.MethodValue;
import com.homing_labs.cq.lang.java.model.Parameters;
import com.homing_labs.cq.lang.java.model.TypeParameterValue;
import com.homing_labs.cq.lang.java.outline.BodyPart;
import com.homing_labs.cq.lang.java.outline.BodyParts;
import com.homing_labs.cq.lang.java.outline.TokenRange;
import com.homing_labs.cq.lang.java.outline.TypeKeyword;
import com.homing_labs.cq.lang.java.outline.TypeParameter;
import hue.captains.singapura.tao.ontology.StatelessFunctionalObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.BiPredicate;

/**
 * Which method or constructor written in a body a class file's one is: of those of its name, the one with the type
 * parameters and the parameters' types its signature declares, matched by the names they are written with.
 */
record Overloads() implements StatelessFunctionalObject {

    static final Overloads INSTANCE = new Overloads();

    /**
     * Where a method is written: of the methods of its name, the one with the type parameters and the parameters'
     * types its signature declares - or, with no signature, its descriptor. Two overloads cannot have one signature;
     * only types written by names too short to tell two apart leave more than one, and then the first written is taken.
     */
    Optional<Candidate> written(final ReadBody body, final MethodValue value) {
        return matching(methods(body.lexed(), body.parts(), value.name()),
                value.signature().map(signature -> variables(signature.typeParameters().items())).orElse(List.of()),
                TypeNames.INSTANCE.qualified(value.signature().map(MethodSignature::parameters)
                        .orElse(value.parameters())))
                .stream().findFirst();
    }

    /** Where a constructor is written, as a method is: by the type parameters and the parameters' types it declares. */
    Optional<Candidate> written(final Declared declared, final ReadBody body, final ConstructorValue value) {
        return matching(constructors(declared, body),
                value.signature().map(signature -> variables(signature.typeParameters().items())).orElse(List.of()),
                value.signature().map(ConstructorSignature::parameters).map(TypeNames.INSTANCE::qualified)
                        .orElseGet(() -> writtenParameters(declared, value.parameters())))
                .stream().findFirst();
    }

    /** The methods of a body with the name, each with its type parameters and its parameters' types as written. */
    List<Candidate> methods(final Lexed lexed, final BodyParts parts, final String name) {
        return parts.of(BodyPart.Method.class).stream()
                .filter(method -> method.name().text().equals(name))
                .map(method -> new Candidate(lexed.trimmed(method.range(), false),
                        variables(lexed, method.typeParameters().items()),
                        TypeNames.INSTANCE.writtenTypes(lexed, method.parameters().formal()), method.body(), method))
                .toList();
    }

    /**
     * The constructors of a body - a record's compact one taking its components - each with its type parameters and its
     * parameters' types.
     */
    List<Candidate> constructors(final Declared declared, final ReadBody body) {
        final Lexed lexed = body.lexed();
        final List<Candidate> found = new ArrayList<>();
        for (final BodyPart part : body.parts().items()) {
            switch (part) {
                case BodyPart.Constructor constructor -> found.add(new Candidate(
                        lexed.trimmed(constructor.range(), false),
                        variables(lexed, constructor.typeParameters().items()),
                        TypeNames.INSTANCE.writtenTypes(lexed, constructor.parameters().formal()), constructor.body(),
                        constructor));
                case BodyPart.CompactConstructor compact -> found.add(new Candidate(
                        lexed.trimmed(compact.range(), false), List.of(), componentTypes(declared), Optional.empty(),
                        compact));
                default -> {
                }
            }
        }
        return found;
    }

    List<TokenRange> ranges(final List<Candidate> candidates) {
        return candidates.stream().map(Candidate::range).toList();
    }

    /**
     * The types of a record's components as written, in the tokens its header was read from: a member record's are the
     * body's that declares it, not its own body's.
     */
    private List<String> componentTypes(final Declared declared) {
        return declared.header().components().stream()
                .flatMap(components -> components.items().stream())
                .map(component -> TypeNames.INSTANCE.erased(declared.lexed(), component.type().range(),
                        component.variable() ? 1 : 0))
                .toList();
    }

    /**
     * The candidates that declare the type parameters the class file's signature has, by name and bounds, and take
     * parameters of its types, and of those the ones that name them most fully: {@code PlatformLogger.Level} is the one
     * of two overloads that takes it, and {@code Level}, written in the other, can then only be another {@code Level}.
     * When none fits so, those whose types have the class file's simple names: a member type a class inherits can be
     * written by that class's name, {@code JCMemberReference.ReferenceMode} for
     * {@code MemberReferenceTree.ReferenceMode}. The signature keeps a type variable as it is named, and its bounds:
     * {@code <T extends Number> pick(T)} and {@code <T extends CharSequence> pick(T)} are told apart by T's, and
     * {@code put(T)}, taking its class's T, from {@code <T> put(T)}, which declares its own.
     */
    private List<Candidate> matching(final List<Candidate> candidates, final List<Variable> variables,
                                     final List<String> parameters) {
        final List<Candidate> qualified = candidates.stream()
                .filter(candidate -> fit(candidate, variables, parameters, TypeNames.INSTANCE::fits))
                .toList();
        final List<Candidate> fitting = !qualified.isEmpty() ? qualified : candidates.stream()
                .filter(candidate -> fit(candidate, variables, parameters, TypeNames.INSTANCE::sameSimpleName))
                .toList();
        final int fullest = fitting.stream().mapToInt(this::names).max().orElse(0);
        return fitting.stream().filter(candidate -> names(candidate) == fullest).toList();
    }

    /** Whether a candidate declares the type variables and takes parameters of the types, its names fitting theirs. */
    private boolean fit(final Candidate candidate, final List<Variable> variables, final List<String> parameters,
                        final BiPredicate<String, String> fits) {
        if (candidate.typeParameters().size() != variables.size()) {
            return false;
        }
        for (int at = 0; at < variables.size(); at++) {
            final Variable written = candidate.typeParameters().get(at);
            final Variable compiled = variables.get(at);
            if (!written.name().equals(compiled.name()) || !fit(written.bounds(), compiled.bounds(), fits)) {
                return false;
            }
        }
        return fit(candidate.parameters(), parameters, fits);
    }

    private boolean fit(final List<String> written, final List<String> compiled,
                        final BiPredicate<String, String> fits) {
        if (written.size() != compiled.size()) {
            return false;
        }
        for (int at = 0; at < written.size(); at++) {
            if (!fits.test(written.get(at), compiled.get(at))) {
                return false;
            }
        }
        return true;
    }

    /** Type parameters as a signature declares them: each by its name and its bounds' qualified names. */
    private List<Variable> variables(final List<TypeParameterValue> parameters) {
        return parameters.stream()
                .map(parameter -> new Variable(parameter.name(),
                        parameter.bounds().items().stream().map(TypeNames.INSTANCE::qualified).toList()))
                .toList();
    }

    /**
     * Type parameters as written: each by its name and its bounds by the names they are written with. One written with
     * no bound is bounded by {@code Object}, as a signature has it.
     */
    private List<Variable> variables(final Lexed lexed, final List<TypeParameter> parameters) {
        return parameters.stream()
                .map(parameter -> new Variable(parameter.name().text(), parameter.bounds().items().isEmpty()
                        ? List.of("Object")
                        : parameter.bounds().items().stream()
                        .map(bound -> TypeNames.INSTANCE.erased(lexed, bound.range(), 0)).toList()))
                .toList();
    }

    /** How many names a candidate's parameters' types and its type parameters' bounds are written with, all told. */
    private int names(final Candidate candidate) {
        return names(candidate.parameters()) + candidate.typeParameters().stream()
                .mapToInt(variable -> names(variable.bounds()))
                .sum();
    }

    /** How many names the types are written with, all told. */
    private int names(final List<String> written) {
        return written.stream().mapToInt(type -> type.split("\\.").length).sum();
    }

    /**
     * The types of the parameters a constructor is written with: javac gives an enum's its name and ordinal first, and
     * an inner class's the object of its enclosing class.
     */
    private List<String> writtenParameters(final Declared declared, final Parameters parameters) {
        final List<String> all = TypeNames.INSTANCE.qualified(parameters);
        final int added = declared.header().keyword() instanceof TypeKeyword.Enum ? 2 : declared.inner() ? 1 : 0;
        return all.size() >= added ? all.subList(added, all.size()) : all;
    }
}
