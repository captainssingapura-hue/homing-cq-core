package com.homing_labs.cq.lang.java.taxonomy.scan;

import com.homing_labs.cq.lang.java.taxonomy.AnonymousClass;
import com.homing_labs.cq.lang.java.taxonomy.ClassAnnotation;
import com.homing_labs.cq.lang.java.taxonomy.ClassKind;
import com.homing_labs.cq.lang.java.taxonomy.ClassProperty;
import com.homing_labs.cq.lang.java.taxonomy.Constructor;
import com.homing_labs.cq.lang.java.taxonomy.EnumConstant;
import com.homing_labs.cq.lang.java.taxonomy.Field;
import com.homing_labs.cq.lang.java.taxonomy.LocalClass;
import com.homing_labs.cq.lang.java.taxonomy.MemberClass;
import com.homing_labs.cq.lang.java.taxonomy.Method;
import com.homing_labs.cq.lang.java.taxonomy.ObjectKind;
import com.homing_labs.cq.lang.java.taxonomy.RecordComponent;
import com.homing_labs.cq.lang.java.taxonomy.StaticField;
import com.homing_labs.cq.lang.java.taxonomy.StaticInitialiser;
import com.homing_labs.cq.lang.java.taxonomy.StaticMemberClass;
import com.homing_labs.cq.lang.java.taxonomy.StaticMethod;
import com.homing_labs.cq.lang.java.taxonomy.Superclass;
import com.homing_labs.cq.lang.java.taxonomy.Superinterface;
import com.homing_labs.cq.lang.java.taxonomy.TypeParameter;
import hue.captains.singapura.tao.ontology.StatelessFunctionalObject;

import java.lang.classfile.Attributes;
import java.lang.classfile.ClassFile;
import java.lang.classfile.ClassModel;
import java.lang.classfile.FieldModel;
import java.lang.classfile.MethodModel;
import java.lang.classfile.attribute.InnerClassInfo;
import java.lang.classfile.constantpool.ClassEntry;
import java.lang.reflect.AccessFlag;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Finds every object of the taxonomy in a class file: the class itself, of its kind, and each of its own members. It
 * reads the class file alone, loading nothing, so it finds what the compiler implied and generated as well as what the
 * source declared - and nothing inherited.
 */
public record ClassScanner() implements StatelessFunctionalObject {

    public static final ClassScanner INSTANCE = new ClassScanner();

    /**
     * The objects in one class file: the class, then its members, then its dependencies. A local or anonymous class is
     * found in its own class file, which names the class whose code declares it, and so is listed as that class's.
     */
    public List<Occurrence> scan(final byte[] classFile) {
        final ClassModel model = ClassFile.of().parse(classFile);
        final String name = model.thisClass().asInternalName();
        final ClassKind kind = kindOf(model);
        final List<Occurrence> found = new ArrayList<>();
        found.add(new Occurrence.OfClass(name, kind));
        found.add(new Occurrence.OfMember(name, ClassProperty.Visibility.INSTANCE, visibility(classFlagsOf(model))));
        found.add(new Occurrence.OfMember(name, ClassProperty.Extensibility.INSTANCE, extensibility(model)));
        Stream.concat(
                        model.findAttribute(Attributes.runtimeVisibleAnnotations()).stream()
                                .flatMap(visible -> visible.annotations().stream()),
                        model.findAttribute(Attributes.runtimeInvisibleAnnotations()).stream()
                                .flatMap(invisible -> invisible.annotations().stream()))
                .forEach(annotation -> found.add(new Occurrence.OfMember(name, ClassAnnotation.INSTANCE,
                        annotation.className().stringValue().replaceAll("^L|;$", ""))));
        model.findAttribute(Attributes.signature()).ifPresent(signature -> signature.asClassSignature()
                .typeParameters().forEach(parameter -> found.add(
                        new Occurrence.OfMember(name, TypeParameter.INSTANCE, parameter.identifier()))));
        if (!(kind instanceof ClassKind.Interface || kind instanceof ClassKind.Annotation)) {
            model.superclass().ifPresent(superclass -> found.add(
                    new Occurrence.OfMember(name, Superclass.INSTANCE, superclass.asInternalName())));
        }
        model.interfaces().forEach(superinterface -> found.add(
                new Occurrence.OfMember(name, Superinterface.INSTANCE, superinterface.asInternalName())));
        model.fields().forEach(field -> found.add(
                new Occurrence.OfMember(name, fieldKindOf(field), field.fieldName().stringValue())));
        if (kind instanceof ClassKind.Record) {
            model.findAttribute(Attributes.record()).orElseThrow().components().forEach(component -> found.add(
                    new Occurrence.OfMember(name, RecordComponent.INSTANCE, component.name().stringValue())));
        }
        model.methods().forEach(method -> found.add(member(name, model, method)));
        memberClasses(model).forEach(member -> found.add(
                new Occurrence.OfMember(name, memberClassKindOf(member), member.innerClass().asInternalName())));
        enclosingClassOf(model).ifPresent(enclosing -> found.add(
                new Occurrence.OfMember(enclosing, codeClassKindOf(model).orElseThrow(), name)));
        found.addAll(DependencyFinder.INSTANCE.find(model));
        return List.copyOf(found);
    }

    /**
     * The kind of the class a class file declares, from its flags and its superclass. A class is a record as the JDK
     * has it: final, extending {@code Record}, and with a {@code Record} attribute. On any other class the JDK ignores
     * the attribute, and so does the scanner.
     */
    public ClassKind kindOf(final ClassModel model) {
        final String superclass = model.superclass().map(ClassEntry::asInternalName).orElse("");
        if (model.flags().has(AccessFlag.ANNOTATION)) {
            return ClassKind.Annotation.INSTANCE;
        }
        if (model.flags().has(AccessFlag.INTERFACE)) {
            return ClassKind.Interface.INSTANCE;
        }
        if (model.flags().has(AccessFlag.ENUM) && superclass.equals("java/lang/Enum")) {
            return ClassKind.Enum.INSTANCE;
        }
        if (superclass.equals("java/lang/Record") && model.flags().has(AccessFlag.FINAL)
                && model.findAttribute(Attributes.record()).isPresent()) {
            return ClassKind.Record.INSTANCE;
        }
        return model.flags().has(AccessFlag.ABSTRACT) ? ClassKind.AbstractClass.INSTANCE : ClassKind.Class.INSTANCE;
    }

    /**
     * The flags the class a class file declares was declared with. A nested class's are in the entry its
     * {@code InnerClasses} has for the class itself, since its own flags can only say public or package-private.
     */
    public Set<AccessFlag> classFlagsOf(final ClassModel model) {
        return entryForItself(model).map(InnerClassInfo::flags).orElseGet(() -> model.flags().flags());
    }

    /**
     * The class whose code declares the local or anonymous class a class file declares, by its internal name, as the
     * class file's {@code EnclosingMethod} names it; nothing for any other class.
     */
    public Optional<String> enclosingClassOf(final ClassModel model) {
        return model.findAttribute(Attributes.enclosingMethod())
                .map(enclosing -> enclosing.enclosingClass().asInternalName());
    }

    /**
     * Whether the class a class file declares is a local or an anonymous class: a local class has a name in the entry
     * its {@code InnerClasses} has for itself, and an anonymous one none. Nothing for any other class.
     */
    public Optional<ObjectKind> codeClassKindOf(final ClassModel model) {
        return enclosingClassOf(model).flatMap(enclosing -> entryForItself(model))
                .map(self -> self.innerName().isPresent() ? LocalClass.INSTANCE : AnonymousClass.INSTANCE);
    }

    /**
     * The classes that may be local or anonymous classes of the class a class file declares: those its
     * {@code InnerClasses} lists with no outer class, but itself. Some of them are declared deeper - in the code of
     * another local or anonymous class - which only their own {@link #enclosingClassOf} tells.
     */
    public List<InnerClassInfo> codeClassCandidates(final ClassModel model) {
        final String name = model.thisClass().asInternalName();
        return model.findAttribute(Attributes.innerClasses())
                .map(innerClasses -> innerClasses.classes().stream()
                        .filter(inner -> inner.outerClass().isEmpty())
                        .filter(inner -> !inner.innerClass().asInternalName().equals(name))
                        .toList())
                .orElse(List.of());
    }

    /** The entry a class file's {@code InnerClasses} has for the class it declares, if that class is nested. */
    private Optional<InnerClassInfo> entryForItself(final ClassModel model) {
        final String name = model.thisClass().asInternalName();
        return model.findAttribute(Attributes.innerClasses())
                .flatMap(innerClasses -> innerClasses.classes().stream()
                        .filter(inner -> inner.innerClass().asInternalName().equals(name))
                        .findFirst());
    }

    /** The classes that are members of the class a class file declares, as its {@code InnerClasses} lists them. */
    public List<InnerClassInfo> memberClasses(final ClassModel model) {
        final String name = model.thisClass().asInternalName();
        return model.findAttribute(Attributes.innerClasses())
                .map(innerClasses -> innerClasses.classes().stream()
                        .filter(inner -> inner.outerClass().map(ClassEntry::asInternalName)
                                .filter(name::equals).isPresent())
                        .toList())
                .orElse(List.of());
    }

    /** Which kind of field a field is: an enum constant, which only it is flagged as, or else static or instance. */
    public ObjectKind fieldKindOf(final FieldModel field) {
        if (field.flags().has(AccessFlag.ENUM)) {
            return EnumConstant.INSTANCE;
        }
        return field.flags().has(AccessFlag.STATIC) ? StaticField.INSTANCE : Field.INSTANCE;
    }

    /**
     * Which kind of member class a member class is: static or inner. Its own class file does not say, so this reads the
     * entry of the class it is a member of.
     */
    public ObjectKind memberClassKindOf(final InnerClassInfo member) {
        return member.flags().contains(AccessFlag.STATIC) ? StaticMemberClass.INSTANCE : MemberClass.INSTANCE;
    }

    /**
     * Whether the class a class file declares is sealed: it names the classes it permits. A class's flags cannot say
     * so.
     */
    public boolean isSealed(final ClassModel model) {
        return model.findAttribute(Attributes.permittedSubclasses()).isPresent();
    }

    /** Who can reach a class or a member, in a word: {@code public}, {@code package}. */
    private String visibility(final Set<AccessFlag> flags) {
        return Stream.of(AccessFlag.PUBLIC, AccessFlag.PROTECTED, AccessFlag.PRIVATE)
                .filter(flags::contains)
                .map(flag -> flag.name().toLowerCase())
                .findFirst()
                .orElse("package");
    }

    /** Who can extend a class, in a word: {@code final}, {@code sealed} or {@code open}. */
    private String extensibility(final ClassModel model) {
        return classFlagsOf(model).contains(AccessFlag.FINAL) ? "final" : isSealed(model) ? "sealed" : "open";
    }

    private Occurrence member(final String name, final ClassModel model, final MethodModel method) {
        final String methodName = method.methodName().stringValue();
        final String signature = methodName + method.methodType().stringValue();
        return switch (methodName) {
            case "<init>" -> new Occurrence.OfMember(name, Constructor.INSTANCE, signature);
            case "<clinit>" -> new Occurrence.OfMember(name, StaticInitialiser.INSTANCE, signature);
            default -> new Occurrence.OfMember(name, methodKindOf(model, method), signature);
        };
    }

    /**
     * Which kind of method a method of the class is, other than a constructor or the static initialiser: static or
     * instance, and a declaration, an implementation, a default or a native one.
     */
    public ObjectKind methodKindOf(final ClassModel model, final MethodModel method) {
        if (method.flags().has(AccessFlag.STATIC)) {
            return method.flags().has(AccessFlag.NATIVE) ? StaticMethod.NativeImplementation.INSTANCE
                    : StaticMethod.Implementation.INSTANCE;
        }
        if (method.flags().has(AccessFlag.ABSTRACT)) {
            return Method.Declaration.INSTANCE;
        }
        if (method.flags().has(AccessFlag.NATIVE)) {
            return Method.NativeImplementation.INSTANCE;
        }
        final boolean ofAnInterface = model.flags().has(AccessFlag.INTERFACE);
        return ofAnInterface && !method.flags().has(AccessFlag.PRIVATE) ? Method.DefaultImplementation.INSTANCE
                : Method.Implementation.INSTANCE;
    }
}
