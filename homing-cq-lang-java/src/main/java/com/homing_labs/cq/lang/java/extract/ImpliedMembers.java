package com.homing_labs.cq.lang.java.extract;

import com.homing_labs.cq.lang.java.extract.SourceReading.Declared;
import com.homing_labs.cq.lang.java.model.ConstructorSignature;
import com.homing_labs.cq.lang.java.model.ConstructorValue;
import com.homing_labs.cq.lang.java.model.FieldValue;
import com.homing_labs.cq.lang.java.model.MethodValue;
import com.homing_labs.cq.lang.java.outline.CodeBlock;
import com.homing_labs.cq.lang.java.outline.RecordComponent;
import com.homing_labs.cq.lang.java.outline.TypeKeyword;
import hue.captains.singapura.tao.ontology.StatelessFunctionalObject;

import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * The members the language implies from what a class's declaration writes, and the code that implies each (JLS 8.8.9,
 * 8.9.3, 8.10.3): a record's component its field and its accessor; a record's header its {@code equals},
 * {@code hashCode}, {@code toString} and canonical constructor; an enum's header its {@code values()} and
 * {@code valueOf}; and a class's or an enum's header the constructor its body writes none of. It is asked of a member
 * the class file declares and the body does not write, and a member that does not fit one of these exactly, by name
 * and by the types of its parameters, is implied by nothing.
 */
record ImpliedMembers() implements StatelessFunctionalObject {

    static final ImpliedMembers INSTANCE = new ImpliedMembers();

    /** A record's field: the component it is derived from. */
    Optional<CodeBlock> field(final Declared declared, final FieldValue value) {
        return declared.header().keyword() instanceof TypeKeyword.Record ? component(declared, value.name())
                : Optional.empty();
    }

    /**
     * A record's accessor, implied by its component, or its {@code equals}, {@code hashCode} or {@code toString}, by
     * its header; an enum's {@code values()} or {@code valueOf(String)}, by its header.
     */
    Optional<CodeBlock> method(final Declared declared, final MethodValue value) {
        final List<String> parameters = TypeNames.INSTANCE.qualified(value.parameters());
        final String name = value.name();
        return switch (declared.header().keyword()) {
            case TypeKeyword.Record _ -> parameters.isEmpty() && component(declared, name).isPresent()
                    ? component(declared, name)
                    : objectMethod(name, parameters) ? Optional.of(header(declared)) : Optional.empty();
            case TypeKeyword.Enum _ -> name.equals("values") && parameters.isEmpty()
                    || name.equals("valueOf") && parameters.equals(List.of("java.lang.String"))
                    ? Optional.of(header(declared)) : Optional.empty();
            default -> Optional.empty();
        };
    }

    /**
     * A record's canonical constructor, which takes its components, or the constructor of a class or an enum whose
     * body writes none: implied by the header.
     */
    Optional<CodeBlock> constructor(final Declared declared, final ReadBody body, final ConstructorValue value) {
        final boolean implied = switch (declared.header().keyword()) {
            case TypeKeyword.Record _ -> canonical(declared, value);
            case TypeKeyword.Class _, TypeKeyword.Enum _ -> Overloads.INSTANCE.constructors(declared, body).isEmpty();
            default -> false;
        };
        return implied ? Optional.of(header(declared)) : Optional.empty();
    }

    /** Whether a record's method is one every object has that a record implements: by its name and its parameters. */
    private boolean objectMethod(final String name, final List<String> parameters) {
        return (name.equals("toString") || name.equals("hashCode")) && parameters.isEmpty()
                || name.equals("equals") && parameters.equals(List.of("java.lang.Object"));
    }

    /** Whether a record's constructor takes its components: their types, as written, in order. */
    private boolean canonical(final Declared declared, final ConstructorValue value) {
        final List<String> taken = value.signature().map(ConstructorSignature::parameters)
                .map(TypeNames.INSTANCE::qualified)
                .orElseGet(() -> TypeNames.INSTANCE.qualified(value.parameters()));
        final List<String> components = components(declared)
                .map(component -> TypeNames.INSTANCE.erased(declared.lexed(), component.type().range(),
                        component.variable() ? 1 : 0))
                .toList();
        return taken.size() == components.size() && IntStream.range(0, taken.size())
                .allMatch(at -> TypeNames.INSTANCE.fits(components.get(at), taken.get(at))
                        || TypeNames.INSTANCE.sameSimpleName(components.get(at), taken.get(at)));
    }

    /** A record's component of the name, as written. */
    private Optional<CodeBlock> component(final Declared declared, final String name) {
        return components(declared).filter(component -> component.name().text().equals(name)).findFirst()
                .map(component -> declared.lexed().block(component.range()));
    }

    private Stream<RecordComponent> components(final Declared declared) {
        return declared.header().components().stream().flatMap(components -> components.items().stream());
    }

    private CodeBlock header(final Declared declared) {
        return declared.lexed().block(declared.header().range());
    }
}
