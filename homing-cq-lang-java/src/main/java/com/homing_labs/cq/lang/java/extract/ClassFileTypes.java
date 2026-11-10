package com.homing_labs.cq.lang.java.extract;

import com.homing_labs.cq.lang.java.model.ClassName;
import com.homing_labs.cq.lang.java.model.ConstructorSignature;
import com.homing_labs.cq.lang.java.model.MethodSignature;
import com.homing_labs.cq.lang.java.model.Parameters;
import com.homing_labs.cq.lang.java.model.PrimitiveType;
import com.homing_labs.cq.lang.java.model.TypeArgument;
import com.homing_labs.cq.lang.java.model.TypeArguments;
import com.homing_labs.cq.lang.java.model.TypeParameterValue;
import com.homing_labs.cq.lang.java.model.TypeParameters;
import com.homing_labs.cq.lang.java.model.TypeUse;
import com.homing_labs.cq.lang.java.model.TypeUses;
import hue.captains.singapura.tao.ontology.StatelessFunctionalObject;

import java.lang.classfile.AttributedElement;
import java.lang.classfile.Signature;
import java.lang.constant.ClassDesc;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * The types a class file names, as the model has them: erased, as a descriptor names them, or generic, as a
 * {@code Signature} attribute declares them - and the type parameters and the signatures of methods and constructors
 * declared there.
 */
record ClassFileTypes() implements StatelessFunctionalObject {

    static final ClassFileTypes INSTANCE = new ClassFileTypes();

    /** The type a descriptor names, erased. */
    TypeUse typeUse(final ClassDesc type) {
        if (type.isArray()) {
            return new TypeUse.Array(typeUse(type.componentType()));
        }
        if (type.isPrimitive()) {
            return new TypeUse.Primitive(switch (type.descriptorString()) {
                case "Z" -> PrimitiveType.Boolean.INSTANCE;
                case "B" -> PrimitiveType.Byte.INSTANCE;
                case "C" -> PrimitiveType.Char.INSTANCE;
                case "S" -> PrimitiveType.Short.INSTANCE;
                case "I" -> PrimitiveType.Int.INSTANCE;
                case "J" -> PrimitiveType.Long.INSTANCE;
                case "F" -> PrimitiveType.Float.INSTANCE;
                case "D" -> PrimitiveType.Double.INSTANCE;
                default -> throw new IllegalArgumentException("Not a primitive type: " + type.descriptorString());
            });
        }
        return new TypeUse.Named(name(type));
    }

    /** The type a method's descriptor gives as its result, erased: none, for {@code void}. */
    Optional<TypeUse> result(final ClassDesc type) {
        return type.descriptorString().equals("V") ? Optional.empty() : Optional.of(typeUse(type));
    }

    /** The generic type a field or a record component is declared with, when its class file keeps one. */
    Optional<TypeUse> declaredType(final AttributedElement element) {
        return element.findAttribute(java.lang.classfile.Attributes.signature())
                .map(declared -> genericType(declared.asTypeSignature()));
    }

    ConstructorSignature constructorSignature(final java.lang.classfile.MethodSignature declared) {
        return new ConstructorSignature(typeParameters(declared), declaredParameters(declared), thrown(declared));
    }

    MethodSignature methodSignature(final java.lang.classfile.MethodSignature declared) {
        return new MethodSignature(typeParameters(declared), declaredParameters(declared),
                declared.result() instanceof Signature.BaseTypeSig base && base.baseType() == 'V' ? Optional.empty()
                        : Optional.of(genericType(declared.result())),
                thrown(declared));
    }

    /** A type parameter with its bounds, its class bound - if it has one - first, as Java declares them. */
    TypeParameterValue typeParameter(final Signature.TypeParam parameter) {
        return new TypeParameterValue(parameter.identifier(), new TypeUses(
                Stream.concat(parameter.classBound().stream(), parameter.interfaceBounds().stream())
                        .map(this::genericType)
                        .toList()));
    }

    /** A type as a signature declares it. */
    TypeUse genericType(final Signature signature) {
        return switch (signature) {
            case Signature.BaseTypeSig base -> typeUse(ClassDesc.ofDescriptor(String.valueOf(base.baseType())));
            case Signature.TypeVarSig variable -> new TypeUse.Variable(variable.identifier());
            case Signature.ArrayTypeSig array -> new TypeUse.Array(genericType(array.componentSignature()));
            case Signature.ClassTypeSig type -> {
                final Optional<TypeUse.Parameterized> owner = type.outerType().map(this::genericType)
                        .flatMap(outer -> outer instanceof TypeUse.Parameterized parameterized
                                ? Optional.of(parameterized) : Optional.empty());
                yield type.typeArgs().isEmpty() && owner.isEmpty() ? new TypeUse.Named(name(type.classDesc()))
                        : new TypeUse.Parameterized(name(type.classDesc()),
                        new TypeArguments(type.typeArgs().stream().map(this::typeArgument).toList()), owner);
            }
        };
    }

    private TypeParameters typeParameters(final java.lang.classfile.MethodSignature declared) {
        return new TypeParameters(declared.typeParameters().stream().map(this::typeParameter).toList());
    }

    private Parameters declaredParameters(final java.lang.classfile.MethodSignature declared) {
        return new Parameters(declared.arguments().stream().map(this::genericType).toList());
    }

    private TypeUses thrown(final java.lang.classfile.MethodSignature declared) {
        return new TypeUses(declared.throwableSignatures().stream().map(this::genericType).toList());
    }

    /** A type argument, by its wildcard. */
    private TypeArgument typeArgument(final Signature.TypeArg argument) {
        return switch (argument) {
            case Signature.TypeArg.Unbounded _ -> TypeArgument.Unbounded.INSTANCE;
            case Signature.TypeArg.Bounded bounded -> {
                final TypeUse bound = genericType(bounded.boundType());
                yield switch (bounded.wildcardIndicator()) {
                    case EXTENDS -> new TypeArgument.Extends(bound);
                    case SUPER -> new TypeArgument.Super(bound);
                    case NONE -> new TypeArgument.Exact(bound);
                };
            }
        };
    }

    private ClassName name(final ClassDesc type) {
        final String descriptor = type.descriptorString();
        return new ClassName(descriptor.substring(1, descriptor.length() - 1).replace('/', '.'));
    }
}
