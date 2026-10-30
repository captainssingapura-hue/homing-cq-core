package com.homing_labs.cq.lang.java.taxonomy.scan;

import com.homing_labs.cq.lang.java.taxonomy.ClassKind;
import com.homing_labs.cq.lang.java.taxonomy.Constructor;
import com.homing_labs.cq.lang.java.taxonomy.Field;
import com.homing_labs.cq.lang.java.taxonomy.MemberClass;
import com.homing_labs.cq.lang.java.taxonomy.MemberOf;
import com.homing_labs.cq.lang.java.taxonomy.Method;
import com.homing_labs.cq.lang.java.taxonomy.StaticInitialiser;
import com.homing_labs.cq.lang.java.taxonomy.Subordinate;
import com.homing_labs.cq.lang.java.taxonomy.Superclass;
import com.homing_labs.cq.lang.java.taxonomy.Superinterface;
import hue.captains.singapura.tao.ontology.Scalar;
import hue.captains.singapura.tao.ontology.StatelessFunctionalObject;

import java.lang.classfile.Attributes;
import java.lang.classfile.ClassFile;
import java.lang.classfile.ClassModel;
import java.lang.classfile.MethodModel;
import java.lang.classfile.constantpool.ClassEntry;
import java.lang.reflect.AccessFlag;
import java.util.ArrayList;
import java.util.List;

/**
 * Finds every object of the taxonomy in a class file: the class itself, of its kind, and each of its own members, in
 * its place. It reads the class file alone, loading nothing, so it finds what the compiler implied and generated as
 * well as what the source declared - and nothing inherited.
 */
public record ClassScanner() implements StatelessFunctionalObject {

    public static final ClassScanner INSTANCE = new ClassScanner();

    /** The objects in one class file: the class, then its members. */
    public List<Occurrence> scan(final byte[] classFile) {
        final ClassModel model = ClassFile.of().parse(classFile);
        final String name = model.thisClass().asInternalName();
        final ClassKind kind = kindOf(model);
        final Places places = placesOf(kind);
        final List<Occurrence> found = new ArrayList<>();
        found.add(new Occurrence.OfClass(name, kind));
        found.add(new Occurrence.OfMember(name, places.top(), Subordinate.ClassObject.INSTANCE, name));
        found.add(new Occurrence.OfMember(name, places.top(), Subordinate.Instance.INSTANCE, name));
        if (!(kind instanceof ClassKind.Interface || kind instanceof ClassKind.Annotation)) {
            model.superclass().ifPresent(superclass -> found.add(
                    new Occurrence.OfMember(name, places.top(), Superclass.INSTANCE, superclass.asInternalName())));
        }
        model.interfaces().forEach(superinterface -> found.add(new Occurrence.OfMember(
                name, places.top(), Superinterface.INSTANCE, superinterface.asInternalName())));
        model.fields().forEach(field -> found.add(new Occurrence.OfMember(name,
                places.of(field.flags().has(AccessFlag.STATIC)), Field.INSTANCE, field.fieldName().stringValue())));
        model.methods().forEach(method -> found.add(member(name, model, places, method)));
        model.findAttribute(Attributes.innerClasses()).ifPresent(innerClasses -> innerClasses.classes().stream()
                .filter(inner -> inner.outerClass().map(ClassEntry::asInternalName).filter(name::equals).isPresent())
                .forEach(inner -> found.add(new Occurrence.OfMember(name,
                        places.of(inner.flags().contains(AccessFlag.STATIC)), MemberClass.INSTANCE,
                        inner.innerClass().asInternalName()))));
        return List.copyOf(found);
    }

    private ClassKind kindOf(final ClassModel model) {
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
        if (superclass.equals("java/lang/Record") && model.findAttribute(Attributes.record()).isPresent()) {
            return ClassKind.Record.INSTANCE;
        }
        return model.flags().has(AccessFlag.ABSTRACT) ? ClassKind.AbstractClass.INSTANCE : ClassKind.Class.INSTANCE;
    }

    private Occurrence member(final String name, final ClassModel model, final Places places,
                              final MethodModel method) {
        final String methodName = method.methodName().stringValue();
        final String signature = methodName + method.methodType().stringValue();
        return switch (methodName) {
            case "<init>" -> new Occurrence.OfMember(name, places.top(), Constructor.INSTANCE, signature);
            case "<clinit>" -> new Occurrence.OfMember(name, places.top(), StaticInitialiser.INSTANCE, signature);
            default -> new Occurrence.OfMember(name, places.of(method.flags().has(AccessFlag.STATIC)),
                    methodKindOf(model, method), signature);
        };
    }

    private Method methodKindOf(final ClassModel model, final MethodModel method) {
        if (method.flags().has(AccessFlag.ABSTRACT)) {
            return Method.Declaration.INSTANCE;
        }
        if (method.flags().has(AccessFlag.NATIVE)) {
            return Method.NativeImplementation.INSTANCE;
        }
        final boolean ofAnInterface = model.flags().has(AccessFlag.INTERFACE);
        final boolean shared = !method.flags().has(AccessFlag.STATIC) && !method.flags().has(AccessFlag.PRIVATE);
        return ofAnInterface && shared ? Method.DefaultImplementation.INSTANCE : Method.Implementation.INSTANCE;
    }

    private Places placesOf(final ClassKind kind) {
        return switch (kind) {
            case ClassKind.Class _ -> new Places(MemberOf.Class.class,
                    MemberOf.Class.ClassObject.class, MemberOf.Class.Instance.class);
            case ClassKind.AbstractClass _ -> new Places(MemberOf.AbstractClass.class,
                    MemberOf.AbstractClass.ClassObject.class, MemberOf.AbstractClass.Instance.class);
            case ClassKind.Interface _ -> new Places(MemberOf.Interface.class,
                    MemberOf.Interface.ClassObject.class, MemberOf.Interface.Instance.class);
            case ClassKind.Enum _ -> new Places(MemberOf.Enum.class,
                    MemberOf.Enum.ClassObject.class, MemberOf.Enum.Instance.class);
            case ClassKind.Record _ -> new Places(MemberOf.Record.class,
                    MemberOf.Record.ClassObject.class, MemberOf.Record.Instance.class);
            case ClassKind.Annotation _ -> new Places(MemberOf.Annotation.class,
                    MemberOf.Annotation.ClassObject.class, MemberOf.Annotation.Instance.class);
        };
    }

    /** Where a kind of class keeps its top-level members, and its class object's and its instances'. */
    private record Places(Class<? extends MemberOf> top, Class<? extends MemberOf> classObject,
                          Class<? extends MemberOf> instance) implements Scalar {

        Class<? extends MemberOf> of(final boolean isStatic) {
            return isStatic ? classObject : instance;
        }
    }
}
