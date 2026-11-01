package com.homing_labs.cq.lang.java.extract;

import com.homing_labs.cq.lang.java.model.Annotation;
import com.homing_labs.cq.lang.java.model.Annotations;
import com.homing_labs.cq.lang.java.model.AppliedAnnotation;
import com.homing_labs.cq.lang.java.model.Arity;
import com.homing_labs.cq.lang.java.model.Attributes;
import com.homing_labs.cq.lang.java.model.ClassInitialisation;
import com.homing_labs.cq.lang.java.model.ClassName;
import com.homing_labs.cq.lang.java.model.ClassNames;
import com.homing_labs.cq.lang.java.model.ConstructorSignature;
import com.homing_labs.cq.lang.java.model.ConstructorValue;
import com.homing_labs.cq.lang.java.model.Element;
import com.homing_labs.cq.lang.java.model.ElementValue;
import com.homing_labs.cq.lang.java.model.ElementValues;
import com.homing_labs.cq.lang.java.model.Elements;
import com.homing_labs.cq.lang.java.model.EnclosedClass;
import com.homing_labs.cq.lang.java.model.Enclosure;
import com.homing_labs.cq.lang.java.model.Entries;
import com.homing_labs.cq.lang.java.model.EnumConstantValue;
import com.homing_labs.cq.lang.java.model.Extensibility;
import com.homing_labs.cq.lang.java.model.FieldValue;
import com.homing_labs.cq.lang.java.model.MethodSignature;
import com.homing_labs.cq.lang.java.model.MethodValue;
import com.homing_labs.cq.lang.java.model.Mutability;
import com.homing_labs.cq.lang.java.model.NestedClass;
import com.homing_labs.cq.lang.java.model.Origin;
import com.homing_labs.cq.lang.java.model.Overridability;
import com.homing_labs.cq.lang.java.model.ParameterAnnotation;
import com.homing_labs.cq.lang.java.model.ParameterAnnotations;
import com.homing_labs.cq.lang.java.model.Parameters;
import com.homing_labs.cq.lang.java.model.Persistence;
import com.homing_labs.cq.lang.java.model.PrimitiveType;
import com.homing_labs.cq.lang.java.model.ProcessedSource;
import com.homing_labs.cq.lang.java.model.RecordComponentValue;
import com.homing_labs.cq.lang.java.model.Retention;
import com.homing_labs.cq.lang.java.model.Synchronisation;
import com.homing_labs.cq.lang.java.model.TypeArgument;
import com.homing_labs.cq.lang.java.model.TypeArguments;
import com.homing_labs.cq.lang.java.model.TypeParameterValue;
import com.homing_labs.cq.lang.java.model.TypeParameters;
import com.homing_labs.cq.lang.java.model.TypeUse;
import com.homing_labs.cq.lang.java.model.TypeUses;
import com.homing_labs.cq.lang.java.model.Visibility;
import com.homing_labs.cq.lang.java.model.WithSource;
import com.homing_labs.cq.lang.java.taxonomy.ClassAnnotation;
import com.homing_labs.cq.lang.java.taxonomy.ClassKind;
import com.homing_labs.cq.lang.java.taxonomy.ClassProperty;
import com.homing_labs.cq.lang.java.taxonomy.Constructor;
import com.homing_labs.cq.lang.java.taxonomy.Dependency;
import com.homing_labs.cq.lang.java.taxonomy.EnumConstant;
import com.homing_labs.cq.lang.java.taxonomy.ObjectKind;
import com.homing_labs.cq.lang.java.taxonomy.RecordComponent;
import com.homing_labs.cq.lang.java.taxonomy.Superclass;
import com.homing_labs.cq.lang.java.taxonomy.Superinterface;
import com.homing_labs.cq.lang.java.taxonomy.TypeParameter;
import com.homing_labs.cq.lang.java.taxonomy.scan.ClassScanner;
import com.homing_labs.cq.lang.java.taxonomy.scan.Occurrence;
import hue.captains.singapura.tao.ontology.Scalar;
import hue.captains.singapura.tao.ontology.StatelessFunctionalObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.lang.classfile.AnnotationValue;
import java.lang.classfile.AttributedElement;
import java.lang.classfile.ClassFile;
import java.lang.classfile.ClassModel;
import java.lang.classfile.ClassSignature;
import java.lang.classfile.FieldModel;
import java.lang.classfile.MethodModel;
import java.lang.classfile.Signature;
import java.lang.classfile.attribute.EnclosingMethodAttribute;
import java.lang.classfile.attribute.InnerClassInfo;
import java.lang.classfile.attribute.SignatureAttribute;
import java.lang.classfile.constantpool.ClassEntry;
import java.lang.classfile.constantpool.Utf8Entry;
import java.lang.constant.ClassDesc;
import java.lang.reflect.AccessFlag;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Reads the attributes of a class from its class files - its own, and those of the classes nested in it, members and
 * local and anonymous classes, at any depth - through the class loader that loaded it, loading nothing more. Each
 * member is put under the object of the taxonomy the {@link ClassScanner} says it is, and {@link Entries} shapes what
 * was found into the attributes of the class's kind. A local or anonymous class is the class's when its own class file
 * says it is declared in the class's code; a class the compiler generated is none of the class's, as the scanner
 * lists none. Beside the attributes of each class, how it is initialised: no part of it. The class file has no source,
 * so every value's is unavailable here, until the unit is given its own.
 */
public record ClassFileAttributes() implements StatelessFunctionalObject {

    public static final ClassFileAttributes INSTANCE = new ClassFileAttributes();

    /**
     * The attributes of a class, of any kind, from its class files. No value has its source yet. A class the bootstrap
     * loader loaded has no loader of its own, and its class files are read through the system's.
     */
    public Attributes of(final Class<?> type) {
        return read(loader(type), type.getName().replace('.', '/')).attributes();
    }

    /**
     * How a class is initialised, from its class file: whether code runs - which the source can tell the steps of - or
     * none. It has no source yet.
     */
    public WithSource<ClassInitialisation> initialisation(final Class<?> type) {
        return initialisation(ClassFile.of().parse(bytes(loader(type), type.getName().replace('.', '/'))));
    }

    /** The loader to read a class's class files through: its own, or the system's for one the bootstrap loaded. */
    private ClassLoader loader(final Class<?> type) {
        final ClassLoader loader = type.getClassLoader();
        return loader == null ? ClassLoader.getSystemClassLoader() : loader;
    }

    /** How the class a class file declares is initialised: by code, when it has a static initialiser, or by none. */
    private WithSource<ClassInitialisation> initialisation(final ClassModel parsed) {
        final ClassInitialisation initialisation = parsed.methods().stream()
                .anyMatch(method -> method.methodName().equalsString("<clinit>"))
                ? ClassInitialisation.Code.INSTANCE : ClassInitialisation.None.INSTANCE;
        return new WithSource<>(initialisation, new ProcessedSource.Unavailable<>());
    }

    /** A class from its class file, and its nested classes from theirs. */
    private Read read(final ClassLoader loader, final String internalName) {
        final byte[] classFile = bytes(loader, internalName);
        final ClassModel parsed = ClassFile.of().parse(classFile);
        final ClassKind kind = ClassScanner.INSTANCE.kindOf(parsed);
        final Map<ObjectKind, List<WithSource<? extends Scalar>>> found = new LinkedHashMap<>();
        header(found, parsed, kind);
        fields(found, parsed);
        if (kind instanceof ClassKind.Record) {
            components(found, parsed);
        }
        methods(found, parsed);
        nestedClasses(found, loader, parsed, internalName);
        for (final Occurrence occurrence : ClassScanner.INSTANCE.scan(classFile)) {
            if (occurrence instanceof Occurrence.OfMember member && member.kind() instanceof Dependency) {
                add(found, member.kind(), name(member.name()));
            }
        }
        try {
            return new Read(Entries.INSTANCE.of(kind, found), initialisation(parsed));
        } catch (final IllegalArgumentException e) {
            throw new IllegalArgumentException(name(internalName).binaryName() + ": " + e.getMessage(), e);
        }
    }

    /** What the class's header declares: its visibility and extensibility, annotations, type parameters, supertypes. */
    private void header(final Map<ObjectKind, List<WithSource<? extends Scalar>>> found, final ClassModel parsed,
                        final ClassKind kind) {
        final Set<AccessFlag> classFlags = ClassScanner.INSTANCE.classFlagsOf(parsed);
        add(found, ClassProperty.Visibility.INSTANCE, visibility(classFlags));
        add(found, ClassProperty.Extensibility.INSTANCE,
                classFlags.contains(AccessFlag.FINAL) ? Extensibility.Final.INSTANCE
                        : ClassScanner.INSTANCE.isSealed(parsed) ? Extensibility.Sealed.INSTANCE
                        : Extensibility.Open.INSTANCE);
        annotations(parsed).items().forEach(annotation -> add(found, ClassAnnotation.INSTANCE, annotation));
        final Optional<ClassSignature> classSignature = parsed.findAttribute(java.lang.classfile.Attributes.signature())
                .map(SignatureAttribute::asClassSignature);
        classSignature.ifPresent(declared -> declared.typeParameters()
                .forEach(parameter -> add(found, TypeParameter.INSTANCE, typeParameter(parameter))));
        if (!(kind instanceof ClassKind.Interface || kind instanceof ClassKind.Annotation)) {
            parsed.superclass().ifPresent(superclass -> add(found, Superclass.INSTANCE, classSignature
                    .map(declared -> genericType(declared.superclassSignature()))
                    .orElseGet(() -> new TypeUse.Named(name(superclass)))));
        }
        classSignature.map(declared -> declared.superinterfaceSignatures().stream().map(this::genericType).toList())
                .orElseGet(() -> parsed.interfaces().stream()
                        .<TypeUse>map(superinterface -> new TypeUse.Named(name(superinterface))).toList())
                .forEach(superinterface -> add(found, Superinterface.INSTANCE, superinterface));
    }

    /** The class's fields, its enum constants among them. */
    private void fields(final Map<ObjectKind, List<WithSource<? extends Scalar>>> found, final ClassModel parsed) {
        for (final FieldModel field : parsed.fields()) {
            final Set<AccessFlag> flags = field.flags().flags();
            final ObjectKind fieldKind = ClassScanner.INSTANCE.fieldKindOf(field);
            add(found, fieldKind, fieldKind instanceof EnumConstant
                    ? new EnumConstantValue(field.fieldName().stringValue(), annotations(field))
                    : new FieldValue(field.fieldName().stringValue(), typeUse(field.fieldTypeSymbol()),
                    declaredType(field), visibility(flags),
                    flags.contains(AccessFlag.FINAL) ? Mutability.Final.INSTANCE
                            : flags.contains(AccessFlag.VOLATILE) ? Mutability.Volatile.INSTANCE
                            : Mutability.Mutable.INSTANCE,
                    flags.contains(AccessFlag.TRANSIENT) ? Persistence.Transient.INSTANCE
                            : Persistence.Persistent.INSTANCE,
                    origin(flags), annotations(field)));
        }
    }

    /** A record's components, from its Record attribute. */
    private void components(final Map<ObjectKind, List<WithSource<? extends Scalar>>> found,
                            final ClassModel parsed) {
        parsed.findAttribute(java.lang.classfile.Attributes.record()).orElseThrow().components()
                .forEach(component -> add(found, RecordComponent.INSTANCE, new RecordComponentValue(
                        component.name().stringValue(), typeUse(component.descriptorSymbol()),
                        declaredType(component), annotations(component))));
    }

    /** The class's constructors and methods; its static initialiser is no part of it. */
    private void methods(final Map<ObjectKind, List<WithSource<? extends Scalar>>> found, final ClassModel parsed) {
        for (final MethodModel method : parsed.methods()) {
            final Set<AccessFlag> flags = method.flags().flags();
            final Parameters parameters = new Parameters(
                    method.methodTypeSymbol().parameterList().stream().map(this::typeUse).toList());
            final Optional<java.lang.classfile.MethodSignature> methodSignature =
                    method.findAttribute(java.lang.classfile.Attributes.signature())
                            .map(SignatureAttribute::asMethodSignature);
            switch (method.methodName().stringValue()) {
                case "<init>" -> add(found, Constructor.INSTANCE, new ConstructorValue(parameters, thrown(method),
                        methodSignature.map(this::constructorSignature), visibility(flags), arity(flags),
                        origin(flags), annotations(method), parameterAnnotations(method)));
                case "<clinit>" -> {
                    // how the class is initialised, which is no part of it: read beside its attributes
                }
                default -> add(found, ClassScanner.INSTANCE.methodKindOf(parsed, method), new MethodValue(
                        method.methodName().stringValue(), parameters, result(method.methodTypeSymbol().returnType()),
                        thrown(method), methodSignature.map(this::methodSignature), visibility(flags),
                        flags.contains(AccessFlag.FINAL) ? Overridability.Final.INSTANCE
                                : Overridability.Overridable.INSTANCE,
                        flags.contains(AccessFlag.SYNCHRONIZED) ? Synchronisation.Synchronised.INSTANCE
                                : Synchronisation.Unsynchronised.INSTANCE,
                        arity(flags), origin(flags), annotations(method), parameterAnnotations(method),
                        method.findAttribute(java.lang.classfile.Attributes.annotationDefault())
                                .map(declared -> value(declared.defaultValue()))));
            }
        }
    }

    /**
     * The classes nested in the class, each read from its own class file: its member classes, and the local and
     * anonymous classes its code declares. A deeper class its InnerClasses attribute lists is left to the class it
     * belongs to.
     */
    private void nestedClasses(final Map<ObjectKind, List<WithSource<? extends Scalar>>> found,
                               final ClassLoader loader, final ClassModel parsed, final String internalName) {
        for (final InnerClassInfo member : ClassScanner.INSTANCE.memberClasses(parsed)) {
            final String memberName = member.innerClass().asInternalName();
            final Read read = read(loader, memberName);
            add(found, ClassScanner.INSTANCE.memberClassKindOf(member),
                    new NestedClass(name(memberName), read.attributes(), read.initialisation()));
        }
        for (final InnerClassInfo candidate : ClassScanner.INSTANCE.codeClassCandidates(parsed)) {
            final String candidateName = candidate.innerClass().asInternalName();
            final ClassModel declared = ClassFile.of().parse(bytes(loader, candidateName));
            if (ClassScanner.INSTANCE.enclosingClassOf(declared).filter(internalName::equals).isPresent()) {
                final Read read = read(loader, candidateName);
                add(found, ClassScanner.INSTANCE.codeClassKindOf(declared).orElseThrow(), new EnclosedClass(
                        name(candidateName), enclosure(declared), read.attributes(), read.initialisation()));
            }
        }
    }

    /** A class as its class files have it: its attributes, and how it is initialised beside them. */
    private record Read(Attributes attributes, WithSource<ClassInitialisation> initialisation) {
    }

    /** A value found for an object, without its source: the class file alone does not have it. */
    private void add(final Map<ObjectKind, List<WithSource<? extends Scalar>>> found, final ObjectKind object,
                     final Scalar value) {
        found.computeIfAbsent(object, _ -> new ArrayList<>())
                .add(new WithSource<>(value, new ProcessedSource.Unavailable<>()));
    }

    private byte[] bytes(final ClassLoader loader, final String internalName) {
        try (InputStream in = loader.getResourceAsStream(internalName + ".class")) {
            if (in == null) {
                throw new IllegalStateException("No class file for " + internalName.replace('/', '.'));
            }
            return in.readAllBytes();
        } catch (final IOException e) {
            throw new UncheckedIOException("Cannot read the class file of " + internalName.replace('/', '.'), e);
        }
    }

    private ClassName name(final ClassEntry entry) {
        return name(entry.asInternalName());
    }

    private ClassName name(final String internalName) {
        return new ClassName(internalName.replace('/', '.'));
    }

    /** The annotations a class, a field or a method keeps, those kept at runtime first. */
    private Annotations annotations(final AttributedElement element) {
        final List<AppliedAnnotation> applied = new ArrayList<>();
        element.findAttribute(java.lang.classfile.Attributes.runtimeVisibleAnnotations())
                .ifPresent(visible -> visible.annotations().forEach(annotation -> applied.add(
                        new AppliedAnnotation(annotation(annotation), Retention.AtRuntime.INSTANCE))));
        element.findAttribute(java.lang.classfile.Attributes.runtimeInvisibleAnnotations())
                .ifPresent(invisible -> invisible.annotations().forEach(annotation -> applied.add(
                        new AppliedAnnotation(annotation(annotation), Retention.InClassFile.INSTANCE))));
        return new Annotations(applied);
    }

    /** The classes a method or a constructor throws, as its {@code Exceptions} attribute names them. */
    private ClassNames thrown(final MethodModel method) {
        return new ClassNames(method.findAttribute(java.lang.classfile.Attributes.exceptions())
                .map(exceptions -> exceptions.exceptions().stream().map(this::name).toList())
                .orElse(List.of()));
    }

    /** The annotations a method or a constructor keeps on its parameters, by position, those kept at runtime first. */
    private ParameterAnnotations parameterAnnotations(final MethodModel method) {
        final List<ParameterAnnotation> applied = new ArrayList<>();
        method.findAttribute(java.lang.classfile.Attributes.runtimeVisibleParameterAnnotations())
                .ifPresent(visible -> applyTo(applied, visible.parameterAnnotations(), Retention.AtRuntime.INSTANCE));
        method.findAttribute(java.lang.classfile.Attributes.runtimeInvisibleParameterAnnotations())
                .ifPresent(invisible -> applyTo(applied, invisible.parameterAnnotations(),
                        Retention.InClassFile.INSTANCE));
        return new ParameterAnnotations(applied);
    }

    private void applyTo(final List<ParameterAnnotation> into,
                         final List<List<java.lang.classfile.Annotation>> byPosition, final Retention retention) {
        for (int position = 0; position < byPosition.size(); position++) {
            for (final java.lang.classfile.Annotation annotation : byPosition.get(position)) {
                into.add(new ParameterAnnotation(position, new AppliedAnnotation(annotation(annotation), retention)));
            }
        }
    }

    private Annotation annotation(final java.lang.classfile.Annotation annotation) {
        return new Annotation(className(annotation.className()), new Elements(annotation.elements().stream()
                .map(element -> new Element(element.name().stringValue(), value(element.value())))
                .toList()));
    }

    /** The value of an annotation's element, or of an element's default. */
    private ElementValue value(final AnnotationValue value) {
        return switch (value) {
            case AnnotationValue.OfString text -> new ElementValue.OfString(text.stringValue());
            case AnnotationValue.OfBoolean constant ->
                    primitive(PrimitiveType.Boolean.INSTANCE, constant.booleanValue());
            case AnnotationValue.OfByte constant -> primitive(PrimitiveType.Byte.INSTANCE, constant.byteValue());
            case AnnotationValue.OfChar constant -> primitive(PrimitiveType.Char.INSTANCE, constant.charValue());
            case AnnotationValue.OfShort constant -> primitive(PrimitiveType.Short.INSTANCE, constant.shortValue());
            case AnnotationValue.OfInt constant -> primitive(PrimitiveType.Int.INSTANCE, constant.intValue());
            case AnnotationValue.OfLong constant -> primitive(PrimitiveType.Long.INSTANCE, constant.longValue());
            case AnnotationValue.OfFloat constant -> primitive(PrimitiveType.Float.INSTANCE, constant.floatValue());
            case AnnotationValue.OfDouble constant -> primitive(PrimitiveType.Double.INSTANCE, constant.doubleValue());
            case AnnotationValue.OfEnum constant ->
                    new ElementValue.OfEnum(className(constant.className()), constant.constantName().stringValue());
            case AnnotationValue.OfClass literal -> new ElementValue.OfClass(literal.className().equalsString("V")
                    ? Optional.empty()
                    : Optional.of(typeUse(ClassDesc.ofDescriptor(literal.className().stringValue()))));
            case AnnotationValue.OfAnnotation nested -> new ElementValue.OfAnnotation(annotation(nested.annotation()));
            case AnnotationValue.OfArray array ->
                    new ElementValue.OfArray(new ElementValues(array.values().stream().map(this::value).toList()));
        };
    }

    private ElementValue primitive(final PrimitiveType type, final Object constant) {
        return new ElementValue.OfPrimitive(type, String.valueOf(constant));
    }

    /** A class named by its descriptor, as an annotation's type or an enum's is: {@code Ljava/lang/Deprecated;}. */
    private ClassName className(final Utf8Entry descriptor) {
        final String text = descriptor.stringValue();
        return name(text.substring(1, text.length() - 1));
    }

    /** Where in its class's code a local or anonymous class is declared, as its {@code EnclosingMethod} says. */
    private Enclosure enclosure(final ClassModel declared) {
        final EnclosingMethodAttribute enclosing =
                declared.findAttribute(java.lang.classfile.Attributes.enclosingMethod()).orElseThrow();
        return enclosing.enclosingMethodName().<Enclosure>map(method -> {
            final Parameters parameters = new Parameters(enclosing.enclosingMethodTypeSymbol().orElseThrow()
                    .parameterList().stream().map(this::typeUse).toList());
            return method.equalsString("<init>") ? new Enclosure.InConstructor(parameters)
                    : new Enclosure.InMethod(method.stringValue(), parameters);
        }).orElse(Enclosure.InInitialiser.INSTANCE);
    }

    private Optional<TypeUse> result(final ClassDesc type) {
        return type.descriptorString().equals("V") ? Optional.empty() : Optional.of(typeUse(type));
    }

    private TypeUse typeUse(final ClassDesc type) {
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

    private ClassName name(final ClassDesc type) {
        final String descriptor = type.descriptorString();
        return name(descriptor.substring(1, descriptor.length() - 1));
    }

    /** The generic type a field or a record component is declared with, when its class file keeps one. */
    private Optional<TypeUse> declaredType(final AttributedElement element) {
        return element.findAttribute(java.lang.classfile.Attributes.signature())
                .map(declared -> genericType(declared.asTypeSignature()));
    }

    private ConstructorSignature constructorSignature(final java.lang.classfile.MethodSignature declared) {
        return new ConstructorSignature(typeParameters(declared), declaredParameters(declared), thrown(declared));
    }

    private MethodSignature methodSignature(final java.lang.classfile.MethodSignature declared) {
        return new MethodSignature(typeParameters(declared), declaredParameters(declared),
                declared.result() instanceof Signature.BaseTypeSig base && base.baseType() == 'V' ? Optional.empty()
                        : Optional.of(genericType(declared.result())),
                thrown(declared));
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

    /** A type parameter with its bounds, its class bound - if it has one - first, as Java declares them. */
    private TypeParameterValue typeParameter(final Signature.TypeParam parameter) {
        return new TypeParameterValue(parameter.identifier(), new TypeUses(
                Stream.concat(parameter.classBound().stream(), parameter.interfaceBounds().stream())
                        .map(this::genericType)
                        .toList()));
    }

    /** A type as a signature declares it. */
    private TypeUse genericType(final Signature signature) {
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

    private Visibility visibility(final Set<AccessFlag> flags) {
        return flags.contains(AccessFlag.PUBLIC) ? Visibility.Public.INSTANCE
                : flags.contains(AccessFlag.PROTECTED) ? Visibility.Protected.INSTANCE
                : flags.contains(AccessFlag.PRIVATE) ? Visibility.Private.INSTANCE
                : Visibility.Package.INSTANCE;
    }

    private Arity arity(final Set<AccessFlag> flags) {
        return flags.contains(AccessFlag.VARARGS) ? Arity.Variable.INSTANCE : Arity.Fixed.INSTANCE;
    }

    private Origin origin(final Set<AccessFlag> flags) {
        return flags.contains(AccessFlag.SYNTHETIC) || flags.contains(AccessFlag.BRIDGE) ? Origin.Generated.INSTANCE
                : Origin.Declared.INSTANCE;
    }
}
