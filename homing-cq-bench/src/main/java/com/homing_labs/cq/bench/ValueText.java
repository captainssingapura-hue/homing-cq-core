package com.homing_labs.cq.bench;

import com.homing_labs.cq.lang.java.model.Annotation;
import com.homing_labs.cq.lang.java.model.Annotations;
import com.homing_labs.cq.lang.java.model.AppliedAnnotation;
import com.homing_labs.cq.lang.java.model.ClassName;
import com.homing_labs.cq.lang.java.model.ConstructorSignature;
import com.homing_labs.cq.lang.java.model.ConstructorValue;
import com.homing_labs.cq.lang.java.model.ElementValue;
import com.homing_labs.cq.lang.java.model.EnclosedClass;
import com.homing_labs.cq.lang.java.model.Enclosure;
import com.homing_labs.cq.lang.java.model.EnumConstantValue;
import com.homing_labs.cq.lang.java.model.FieldValue;
import com.homing_labs.cq.lang.java.model.InitialiserValue;
import com.homing_labs.cq.lang.java.model.MethodSignature;
import com.homing_labs.cq.lang.java.model.MethodValue;
import com.homing_labs.cq.lang.java.model.NestedClass;
import com.homing_labs.cq.lang.java.model.Parameters;
import com.homing_labs.cq.lang.java.model.RecordComponentValue;
import com.homing_labs.cq.lang.java.model.Retention;
import com.homing_labs.cq.lang.java.model.TypeArgument;
import com.homing_labs.cq.lang.java.model.TypeParameterValue;
import com.homing_labs.cq.lang.java.model.TypeParameters;
import com.homing_labs.cq.lang.java.model.TypeUse;
import hue.captains.singapura.tao.ontology.Scalar;
import hue.captains.singapura.tao.ontology.StatelessFunctionalObject;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * A value of a class's attributes in a line of text, as the bench shows it: a member by its name and types, then its
 * classifications in words - {@code size(): int - public, overridable, unsynchronised, fixed, declared}; a class by its
 * binary name, or, nested in the unit, without its package; a supertype or a type parameter as Java writes it; an
 * annotation as Java writes it, with where it is kept; a classification by itself - {@code sealed}. A member with a
 * generic signature is shown with its types as declared, not as they run.
 */
public record ValueText() implements StatelessFunctionalObject {

    public static final ValueText INSTANCE = new ValueText();

    private static final TypeUse OBJECT = new TypeUse.Named(new ClassName(Object.class.getName()));

    /** The value in a line. */
    public String of(final Scalar value) {
        return switch (value) {
            case ClassName name -> name.binaryName();
            case TypeUse type -> type(type);
            case TypeParameterValue parameter -> typeParameter(parameter);
            case NestedClass nested -> nested.name().simpleName();
            case EnclosedClass enclosed -> enclosed.name().simpleName() + " " + enclosure(enclosed.enclosure());
            case AppliedAnnotation applied -> annotation(applied.annotation()) + " - " + retention(applied.retention());
            case EnumConstantValue constant -> annotations(constant.annotations()) + constant.name();
            case RecordComponentValue component -> annotations(component.annotations()) + component.name() + ": "
                    + type(component.signature().orElse(component.type()));
            case FieldValue field -> annotations(field.annotations()) + field.name() + ": "
                    + type(field.signature().orElse(field.type())) + " - "
                    + words(field.visibility(), field.mutability(), field.persistence(), field.origin());
            case MethodValue method -> annotations(method.annotations())
                    + method.signature().map(signature -> typeParameters(signature.typeParameters())).orElse("")
                    + method.name()
                    + parameters(method.signature().map(MethodSignature::parameters).orElse(method.parameters()))
                    + ": " + method.signature().map(MethodSignature::result).orElse(method.result())
                    .map(this::type).orElse("void")
                    + method.defaultValue().map(given -> " default " + value(given)).orElse("") + " - "
                    + words(method.visibility(), method.overridability(), method.synchronisation(), method.arity(),
                    method.origin());
            case ConstructorValue constructor -> annotations(constructor.annotations())
                    + constructor.signature().map(signature -> typeParameters(signature.typeParameters())).orElse("")
                    + parameters(constructor.signature().map(ConstructorSignature::parameters)
                    .orElse(constructor.parameters()))
                    + " - " + words(constructor.visibility(), constructor.arity(), constructor.origin());
            case InitialiserValue initialiser -> words(initialiser.origin());
            default -> word(value);
        };
    }

    /** The annotations on a member, each as Java writes it, before the member: {@code @Deprecated }. */
    private String annotations(final Annotations annotations) {
        return annotations.items().stream()
                .map(applied -> annotation(applied.annotation()) + " ")
                .collect(Collectors.joining());
    }

    /** An annotation as Java writes it: {@code @SampleAnnotation(value = "class")}. */
    private String annotation(final Annotation annotation) {
        return "@" + annotation.type().simpleName() + (annotation.elements().items().isEmpty() ? ""
                : annotation.elements().items().stream()
                .map(element -> element.name() + " = " + value(element.value()))
                .collect(Collectors.joining(", ", "(", ")")));
    }

    private String value(final ElementValue value) {
        return switch (value) {
            case ElementValue.OfPrimitive primitive -> primitive.value();
            case ElementValue.OfString text -> "\"" + text.value() + "\"";
            case ElementValue.OfEnum constant -> constant.type().simpleName() + "." + constant.constant();
            case ElementValue.OfClass literal -> literal.type().map(this::type).orElse("void") + ".class";
            case ElementValue.OfAnnotation nested -> annotation(nested.annotation());
            case ElementValue.OfArray array -> array.values().items().stream()
                    .map(this::value)
                    .collect(Collectors.joining(", ", "{", "}"));
        };
    }

    private String retention(final Retention retention) {
        return switch (retention) {
            case Retention.AtRuntime _ -> "kept at runtime";
            case Retention.InClassFile _ -> "kept in the class file";
        };
    }

    private String enclosure(final Enclosure enclosure) {
        return switch (enclosure) {
            case Enclosure.InMethod method -> "in " + method.name() + parameters(method.parameters());
            case Enclosure.InConstructor constructor -> "in the constructor " + parameters(constructor.parameters());
            case Enclosure.InInitialiser _ -> "in an initialiser";
        };
    }

    private String parameters(final Parameters parameters) {
        return parameters.items().stream().map(this::type).collect(Collectors.joining(", ", "(", ")"));
    }

    /** A type as Java writes it, without packages: {@code Map<String, ? super Integer>}, {@code Box<T>.Inner<U>}. */
    private String type(final TypeUse type) {
        return switch (type) {
            case TypeUse.Primitive primitive -> word(primitive.type());
            case TypeUse.Named named -> named.name().simpleName();
            case TypeUse.Parameterized parameterized -> parameterized.owner()
                    .map(owner -> type(owner) + "." + parameterized.name().simpleName()
                            .substring(parameterized.name().simpleName().lastIndexOf('$') + 1))
                    .orElse(parameterized.name().simpleName())
                    + (parameterized.arguments().items().isEmpty() ? "" : parameterized.arguments().items().stream()
                    .map(this::typeArgument)
                    .collect(Collectors.joining(", ", "<", ">")));
            case TypeUse.Variable variable -> variable.name();
            case TypeUse.Array array -> type(array.component()) + "[]";
        };
    }

    private String typeArgument(final TypeArgument argument) {
        return switch (argument) {
            case TypeArgument.Exact exact -> type(exact.type());
            case TypeArgument.Extends bounded -> "? extends " + type(bounded.bound());
            case TypeArgument.Super bounded -> "? super " + type(bounded.bound());
            case TypeArgument.Unbounded _ -> "?";
        };
    }

    /** Type parameters as Java writes them, before what declares them: {@code <K, V extends K> }. */
    private String typeParameters(final TypeParameters parameters) {
        return parameters.items().isEmpty() ? "" : parameters.items().stream()
                .map(this::typeParameter)
                .collect(Collectors.joining(", ", "<", "> "));
    }

    /** A type parameter as Java writes it, leaving out the bound {@code Object} that the class file writes. */
    private String typeParameter(final TypeParameterValue parameter) {
        return parameter.name() + (parameter.bounds().items().equals(List.of(OBJECT)) ? ""
                : parameter.bounds().items().stream()
                .map(this::type)
                .collect(Collectors.joining(" & ", " extends ", "")));
    }

    private String words(final Object... classifications) {
        return Arrays.stream(classifications).map(this::word).collect(Collectors.joining(", "));
    }

    /** A stateless value by the name of its type, in lower case: {@code public}, {@code int}. */
    private String word(final Object value) {
        return value.getClass().getSimpleName().toLowerCase();
    }
}
