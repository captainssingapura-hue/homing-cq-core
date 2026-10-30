package com.homing_labs.cq.lang.java.taxonomy.scan;

import com.homing_labs.cq.lang.java.taxonomy.Dependency;
import com.homing_labs.cq.lang.java.taxonomy.MemberOf;
import hue.captains.singapura.tao.ontology.StatelessFunctionalObject;

import java.lang.classfile.Annotation;
import java.lang.classfile.AnnotationValue;
import java.lang.classfile.AttributedElement;
import java.lang.classfile.Attributes;
import java.lang.classfile.ClassModel;
import java.lang.classfile.CodeElement;
import java.lang.classfile.CodeModel;
import java.lang.classfile.Signature;
import java.lang.classfile.attribute.SignatureAttribute;
import java.lang.classfile.attribute.StackMapFrameInfo;
import java.lang.classfile.constantpool.ClassEntry;
import java.lang.classfile.constantpool.PoolEntry;
import java.lang.classfile.instruction.ConstantInstruction;
import java.lang.classfile.instruction.FieldInstruction;
import java.lang.classfile.instruction.InvokeDynamicInstruction;
import java.lang.classfile.instruction.InvokeInstruction;
import java.lang.classfile.instruction.LocalVariable;
import java.lang.classfile.instruction.LocalVariableType;
import java.lang.classfile.instruction.NewMultiArrayInstruction;
import java.lang.classfile.instruction.NewObjectInstruction;
import java.lang.classfile.instruction.NewReferenceArrayInstruction;
import java.lang.classfile.instruction.TypeCheckInstruction;
import java.lang.constant.ClassDesc;
import java.lang.constant.ConstantDesc;
import java.lang.constant.DirectMethodHandleDesc;
import java.lang.constant.DynamicConstantDesc;
import java.lang.constant.MethodTypeDesc;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;

/**
 * Finds the dependencies in a class file: each other class it names, by where it names it. A class named by an entry
 * that nothing else in the class file uses is the class of a copied constant.
 */
record DependencyFinder() implements StatelessFunctionalObject {

    static final DependencyFinder INSTANCE = new DependencyFinder();

    /** The dependencies of the class in a class file, as top-level members of it, in the place given. */
    List<Occurrence> find(final ClassModel model, final Class<? extends MemberOf> top) {
        final Map<Dependency, Set<String>> found = new LinkedHashMap<>();
        Stream.of(Dependency.Signature.INSTANCE, Dependency.Annotation.INSTANCE, Dependency.Code.INSTANCE,
                        Dependency.LocalVariable.INSTANCE, Dependency.Nesting.INSTANCE,
                        Dependency.PermittedSubclass.INSTANCE, Dependency.InlinedConstant.INSTANCE)
                .forEach(where -> found.put(where, new TreeSet<>()));
        declarations(model, found);
        annotations(model, found);
        model.fields().forEach(field -> annotations(field, found));
        model.methods().forEach(method -> {
            annotations(method, found);
            method.code().ifPresent(code -> code(code, found));
        });
        nesting(model, found.get(Dependency.Nesting.INSTANCE));
        model.findAttribute(Attributes.permittedSubclasses()).ifPresent(permitted -> permitted.permittedSubclasses()
                .forEach(subclass -> add(subclass, found.get(Dependency.PermittedSubclass.INSTANCE))));
        inlinedConstants(model, found);
        final String self = model.thisClass().asInternalName();
        final List<Occurrence> dependencies = new ArrayList<>();
        found.forEach((where, names) -> names.stream()
                .filter(name -> !name.equals(self))
                .forEach(name -> dependencies.add(new Occurrence.OfMember(self, top, where, name))));
        return dependencies;
    }

    private void declarations(final ClassModel model, final Map<Dependency, Set<String>> found) {
        final Set<String> into = found.get(Dependency.Signature.INSTANCE);
        model.findAttribute(Attributes.signature()).map(SignatureAttribute::asClassSignature).ifPresent(signature -> {
            typeParameters(signature.typeParameters(), into);
            Stream.concat(Stream.of(signature.superclassSignature()), signature.superinterfaceSignatures().stream())
                    .forEach(supertype -> supertype.typeArgs().forEach(argument -> add(argument, into)));
        });
        model.fields().forEach(field -> {
            add(field.fieldTypeSymbol(), into);
            field.findAttribute(Attributes.signature()).ifPresent(signature -> add(signature.asTypeSignature(), into));
        });
        model.methods().forEach(method -> {
            add(method.methodTypeSymbol(), into);
            method.findAttribute(Attributes.exceptions())
                    .ifPresent(exceptions -> exceptions.exceptions().forEach(thrown -> add(thrown, into)));
            method.findAttribute(Attributes.signature()).map(SignatureAttribute::asMethodSignature)
                    .ifPresent(signature -> {
                        typeParameters(signature.typeParameters(), into);
                        signature.arguments().forEach(argument -> add(argument, into));
                        add(signature.result(), into);
                        signature.throwableSignatures().forEach(thrown -> add(thrown, into));
                    });
        });
        model.findAttribute(Attributes.record()).ifPresent(record -> record.components().forEach(component -> {
            add(component.descriptorSymbol(), into);
            component.findAttribute(Attributes.signature())
                    .ifPresent(signature -> add(signature.asTypeSignature(), into));
            annotations(component, found);
        }));
    }

    private void annotations(final AttributedElement element, final Map<Dependency, Set<String>> found) {
        final Set<String> into = found.get(Dependency.Annotation.INSTANCE);
        element.findAttribute(Attributes.runtimeVisibleAnnotations())
                .ifPresent(kept -> kept.annotations().forEach(annotation -> add(annotation, into)));
        element.findAttribute(Attributes.runtimeInvisibleAnnotations())
                .ifPresent(kept -> kept.annotations().forEach(annotation -> add(annotation, into)));
        element.findAttribute(Attributes.runtimeVisibleParameterAnnotations()).ifPresent(kept -> kept
                .parameterAnnotations().forEach(parameter -> parameter.forEach(annotation -> add(annotation, into))));
        element.findAttribute(Attributes.runtimeInvisibleParameterAnnotations()).ifPresent(kept -> kept
                .parameterAnnotations().forEach(parameter -> parameter.forEach(annotation -> add(annotation, into))));
        element.findAttribute(Attributes.runtimeVisibleTypeAnnotations())
                .ifPresent(kept -> kept.annotations().forEach(annotation -> add(annotation.annotation(), into)));
        element.findAttribute(Attributes.runtimeInvisibleTypeAnnotations())
                .ifPresent(kept -> kept.annotations().forEach(annotation -> add(annotation.annotation(), into)));
        element.findAttribute(Attributes.annotationDefault()).ifPresent(given -> add(given.defaultValue(), into));
    }

    private void code(final CodeModel code, final Map<Dependency, Set<String>> found) {
        final Set<String> into = found.get(Dependency.Code.INSTANCE);
        final Set<String> locals = found.get(Dependency.LocalVariable.INSTANCE);
        for (final CodeElement element : code) {
            switch (element) {
                case FieldInstruction field -> {
                    add(field.owner(), into);
                    add(field.typeSymbol(), into);
                }
                case InvokeInstruction invoke -> {
                    add(invoke.owner(), into);
                    add(invoke.typeSymbol(), into);
                }
                case InvokeDynamicInstruction dynamic -> {
                    add(dynamic.bootstrapMethod(), into);
                    add(dynamic.typeSymbol(), into);
                    dynamic.bootstrapArgs().forEach(argument -> add(argument, into));
                }
                case NewObjectInstruction made -> add(made.className(), into);
                case NewReferenceArrayInstruction array -> add(array.componentType(), into);
                case NewMultiArrayInstruction array -> add(array.arrayType(), into);
                case TypeCheckInstruction check -> add(check.type(), into);
                case ConstantInstruction constant -> add(constant.constantValue(), into);
                case LocalVariable local -> add(local.typeSymbol(), locals);
                case LocalVariableType local -> add(local.signatureSymbol(), locals);
                default -> {
                }
            }
        }
        code.exceptionHandlers().forEach(handler -> handler.catchType().ifPresent(caught -> add(caught, into)));
        code.findAttribute(Attributes.stackMapTable()).ifPresent(table -> table.entries().forEach(frame -> Stream
                .concat(frame.locals().stream(), frame.stack().stream())
                .forEach(type -> {
                    if (type instanceof StackMapFrameInfo.ObjectVerificationTypeInfo object) {
                        add(object.className(), into);
                    }
                })));
        annotations(code, found);
    }

    private void nesting(final ClassModel model, final Set<String> into) {
        model.findAttribute(Attributes.nestHost()).ifPresent(host -> add(host.nestHost(), into));
        model.findAttribute(Attributes.nestMembers())
                .ifPresent(nest -> nest.nestMembers().forEach(member -> add(member, into)));
        model.findAttribute(Attributes.innerClasses()).ifPresent(nested -> nested.classes().forEach(inner -> {
            add(inner.innerClass(), into);
            inner.outerClass().ifPresent(outer -> add(outer, into));
        }));
        model.findAttribute(Attributes.enclosingMethod()).ifPresent(enclosing -> add(enclosing.enclosingClass(), into));
    }

    /** The classes the constant pool names that nothing else accounts for: those of constants copied in. */
    private void inlinedConstants(final ClassModel model, final Map<Dependency, Set<String>> found) {
        final Set<String> named = new HashSet<>(Set.of(model.thisClass().asInternalName()));
        found.values().forEach(named::addAll);
        model.superclass().ifPresent(superclass -> add(superclass, named));
        model.interfaces().forEach(superinterface -> add(superinterface, named));
        final Set<String> copied = found.get(Dependency.InlinedConstant.INSTANCE);
        for (final PoolEntry entry : model.constantPool()) {
            if (entry instanceof ClassEntry type) {
                add(type, copied);
            }
        }
        copied.removeAll(named);
    }

    private void add(final Signature signature, final Set<String> into) {
        switch (signature) {
            case Signature.ClassTypeSig type -> {
                into.add(type.className());
                type.outerType().ifPresent(outer -> add(outer, into));
                type.typeArgs().forEach(argument -> add(argument, into));
            }
            case Signature.ArrayTypeSig array -> add(array.componentSignature(), into);
            default -> {
            }
        }
    }

    private void add(final Signature.TypeArg argument, final Set<String> into) {
        if (argument instanceof Signature.TypeArg.Bounded bounded) {
            add(bounded.boundType(), into);
        }
    }

    private void typeParameters(final List<Signature.TypeParam> parameters, final Set<String> into) {
        parameters.forEach(parameter -> {
            parameter.classBound().ifPresent(bound -> add(bound, into));
            parameter.interfaceBounds().forEach(bound -> add(bound, into));
        });
    }

    private void add(final Annotation annotation, final Set<String> into) {
        add(annotation.classSymbol(), into);
        annotation.elements().forEach(element -> add(element.value(), into));
    }

    private void add(final AnnotationValue value, final Set<String> into) {
        switch (value) {
            case AnnotationValue.OfClass type -> add(type.classSymbol(), into);
            case AnnotationValue.OfEnum constant -> add(constant.classSymbol(), into);
            case AnnotationValue.OfAnnotation nested -> add(nested.annotation(), into);
            case AnnotationValue.OfArray array -> array.values().forEach(element -> add(element, into));
            default -> {
            }
        }
    }

    private void add(final ConstantDesc constant, final Set<String> into) {
        switch (constant) {
            case ClassDesc type -> add(type, into);
            case MethodTypeDesc type -> add(type, into);
            case DirectMethodHandleDesc handle -> {
                add(handle.owner(), into);
                add(handle.invocationType(), into);
            }
            case DynamicConstantDesc<?> dynamic -> {
                add(dynamic.bootstrapMethod(), into);
                add(dynamic.constantType(), into);
                dynamic.bootstrapArgsList().forEach(argument -> add(argument, into));
            }
            default -> {
            }
        }
    }

    private void add(final MethodTypeDesc type, final Set<String> into) {
        add(type.returnType(), into);
        type.parameterList().forEach(parameter -> add(parameter, into));
    }

    private void add(final ClassEntry entry, final Set<String> into) {
        add(entry.asSymbol(), into);
    }

    /** The class a type names, an array's element class for an array; a primitive names none. */
    private void add(final ClassDesc type, final Set<String> into) {
        ClassDesc element = type;
        while (element.isArray()) {
            element = element.componentType();
        }
        if (element.isClassOrInterface()) {
            final String descriptor = element.descriptorString();
            into.add(descriptor.substring(1, descriptor.length() - 1));
        }
    }
}
