package com.homing_labs.cq.lang.java.extract;

import com.homing_labs.cq.lang.java.extract.SourceReading.Declared;
import com.homing_labs.cq.lang.java.model.ClassName;
import com.homing_labs.cq.lang.java.model.CodeBlocks;
import com.homing_labs.cq.lang.java.model.ConstructorValue;
import com.homing_labs.cq.lang.java.model.EnumConstantValue;
import com.homing_labs.cq.lang.java.model.FieldValue;
import com.homing_labs.cq.lang.java.model.MethodValue;
import com.homing_labs.cq.lang.java.model.NestedClass;
import com.homing_labs.cq.lang.java.model.Origin;
import com.homing_labs.cq.lang.java.model.ProcessedSource;
import com.homing_labs.cq.lang.java.model.WrittenParameter;
import com.homing_labs.cq.lang.java.model.WrittenParameters;
import com.homing_labs.cq.lang.java.outline.BodyPart;
import com.homing_labs.cq.lang.java.outline.Declarator;
import com.homing_labs.cq.lang.java.outline.Parameter;
import hue.captains.singapura.tao.ontology.StatelessFunctionalObject;

import java.util.List;
import java.util.Optional;

/**
 * The source of what a class's body has as its members: a field, an enum constant, a method, a constructor or a
 * member class, each where the body writes it - or written nowhere, when the compiler generates it.
 */
record MemberSources() implements StatelessFunctionalObject {

    static final MemberSources INSTANCE = new MemberSources();

    ProcessedSource<FieldValue> field(final ReadBody body, final FieldValue value) {
        if (generated(value.origin())) {
            return new ProcessedSource.Unwritten<>();
        }
        final Lexed lexed = body.lexed();
        for (final BodyPart.Field field : body.parts().of(BodyPart.Field.class)) {
            for (final Declarator declarator : field.declarators().items()) {
                if (declarator.name().text().equals(value.name())) {
                    return new ProcessedSource.OfField(lexed.block(lexed.trimmed(field.range(), false)),
                            lexed.doc(field.doc()), lexed.block(declarator.range()),
                            declarator.initializer().map(lexed::block));
                }
            }
        }
        return new ProcessedSource.Unwritten<>();
    }

    ProcessedSource<EnumConstantValue> enumConstant(final ReadBody body, final EnumConstantValue value) {
        final Lexed lexed = body.lexed();
        return body.parts().of(BodyPart.EnumConstant.class).stream()
                .filter(part -> part.name().text().equals(value.name()))
                .findFirst()
                .<ProcessedSource<EnumConstantValue>>map(part -> new ProcessedSource.OfEnumConstant(
                        lexed.block(lexed.trimmed(part.range(), true)), lexed.doc(part.doc()),
                        part.arguments().map(lexed::block), part.body().map(lexed::block)))
                .orElseGet(ProcessedSource.Unwritten::new);
    }

    ProcessedSource<MethodValue> method(final ReadBody body, final MethodValue value) {
        if (generated(value.origin())) {
            return new ProcessedSource.Unwritten<>();
        }
        final Lexed lexed = body.lexed();
        final Optional<Candidate> found = Overloads.INSTANCE.written(body, value);
        if (found.isEmpty() || !(found.get().part() instanceof BodyPart.Method method)) {
            return new ProcessedSource.Unwritten<>();
        }
        return new ProcessedSource.OfMethod(lexed.block(found.get().range()), lexed.doc(method.doc()),
                parameters(lexed, method.parameters().formal()), method.body().map(lexed::block),
                method.defaultValue().map(lexed::block));
    }

    /** A constructor, and, unless it hands over to another, the instance initialisers it runs. */
    ProcessedSource<ConstructorValue> constructor(final Declared declared, final ReadBody body,
                                                  final ConstructorValue value) {
        if (generated(value.origin())) {
            return new ProcessedSource.Unwritten<>();
        }
        final Lexed lexed = body.lexed();
        final Optional<Candidate> found = Overloads.INSTANCE.written(declared, body, value);
        if (found.isEmpty()) {
            return new ProcessedSource.Unwritten<>();
        }
        final Candidate candidate = found.get();
        final CodeBlocks initialisers = Initialisers.INSTANCE.delegates(lexed, candidate) ? new CodeBlocks(List.of())
                : lexed.blocks(Initialisers.INSTANCE.instanceInitialisers(lexed, declared.header(), body.parts()));
        return switch (candidate.part()) {
            case BodyPart.Constructor constructor -> new ProcessedSource.OfConstructor(
                    lexed.block(candidate.range()), lexed.doc(constructor.doc()),
                    parameters(lexed, constructor.parameters().formal()), constructor.body().map(lexed::block),
                    initialisers);
            case BodyPart.CompactConstructor compact -> new ProcessedSource.OfConstructor(
                    lexed.block(candidate.range()), lexed.doc(compact.doc()), componentParameters(declared),
                    Optional.of(lexed.block(compact.body())), initialisers);
            default -> new ProcessedSource.Unwritten<>();
        };
    }

    /** A member class, as the body of the class it is a member of declares it. */
    ProcessedSource<NestedClass> nestedClass(final ReadBody body, final ClassName inClass, final NestedClass nested) {
        final Lexed lexed = body.lexed();
        return body.memberType(inClass, nested.name())
                .<ProcessedSource<NestedClass>>map(type -> new ProcessedSource.OfNestedClass(
                        lexed.block(lexed.trimmed(type.range(), false)), lexed.doc(type.header().doc()),
                        lexed.block(type.header().range()),
                        type.body().isEmpty() ? Optional.empty() : Optional.of(lexed.block(type.body()))))
                .orElseGet(ProcessedSource.Unwritten::new);
    }

    /** A record's components, as the parameters its compact constructor takes. */
    private WrittenParameters componentParameters(final Declared declared) {
        return new WrittenParameters(declared.header().components().stream()
                .flatMap(components -> components.items().stream())
                .map(component -> new WrittenParameter(component.name().text(),
                        declared.lexed().block(component.range())))
                .toList());
    }

    /** Formal parameters as written: each by its name, with its code. */
    private WrittenParameters parameters(final Lexed lexed, final List<Parameter.Formal> parameters) {
        return new WrittenParameters(parameters.stream()
                .map(parameter -> new WrittenParameter(parameter.name().text(), lexed.block(parameter.range())))
                .toList());
    }

    private boolean generated(final Origin origin) {
        return origin instanceof Origin.Generated;
    }
}
