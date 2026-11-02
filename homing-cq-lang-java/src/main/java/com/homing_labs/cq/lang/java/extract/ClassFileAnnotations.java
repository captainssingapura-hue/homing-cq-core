package com.homing_labs.cq.lang.java.extract;

import com.homing_labs.cq.lang.java.model.Annotation;
import com.homing_labs.cq.lang.java.model.Annotations;
import com.homing_labs.cq.lang.java.model.AppliedAnnotation;
import com.homing_labs.cq.lang.java.model.ClassName;
import com.homing_labs.cq.lang.java.model.Element;
import com.homing_labs.cq.lang.java.model.ElementValue;
import com.homing_labs.cq.lang.java.model.ElementValues;
import com.homing_labs.cq.lang.java.model.Elements;
import com.homing_labs.cq.lang.java.model.ParameterAnnotation;
import com.homing_labs.cq.lang.java.model.ParameterAnnotations;
import com.homing_labs.cq.lang.java.model.PrimitiveType;
import com.homing_labs.cq.lang.java.model.Retention;
import hue.captains.singapura.tao.ontology.StatelessFunctionalObject;

import java.lang.classfile.AnnotationValue;
import java.lang.classfile.AttributedElement;
import java.lang.classfile.MethodModel;
import java.lang.classfile.constantpool.Utf8Entry;
import java.lang.constant.ClassDesc;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * The annotations a class file keeps, as the model has them: on a class, a field, a method or a record component, or
 * on the parameters of a method or a constructor, each kept at runtime or in the class file alone; and the values of
 * their elements. Type annotations are left out.
 */
record ClassFileAnnotations() implements StatelessFunctionalObject {

    static final ClassFileAnnotations INSTANCE = new ClassFileAnnotations();

    /** The annotations a class, a field or a method keeps, those kept at runtime first. */
    Annotations annotations(final AttributedElement element) {
        final List<AppliedAnnotation> applied = new ArrayList<>();
        element.findAttribute(java.lang.classfile.Attributes.runtimeVisibleAnnotations())
                .ifPresent(visible -> visible.annotations().forEach(annotation -> applied.add(
                        new AppliedAnnotation(annotation(annotation), Retention.AtRuntime.INSTANCE))));
        element.findAttribute(java.lang.classfile.Attributes.runtimeInvisibleAnnotations())
                .ifPresent(invisible -> invisible.annotations().forEach(annotation -> applied.add(
                        new AppliedAnnotation(annotation(annotation), Retention.InClassFile.INSTANCE))));
        return new Annotations(applied);
    }

    /** The annotations a method or a constructor keeps on its parameters, by position, those kept at runtime first. */
    ParameterAnnotations parameterAnnotations(final MethodModel method) {
        final List<ParameterAnnotation> applied = new ArrayList<>();
        method.findAttribute(java.lang.classfile.Attributes.runtimeVisibleParameterAnnotations())
                .ifPresent(visible -> applyTo(applied, visible.parameterAnnotations(), Retention.AtRuntime.INSTANCE));
        method.findAttribute(java.lang.classfile.Attributes.runtimeInvisibleParameterAnnotations())
                .ifPresent(invisible -> applyTo(applied, invisible.parameterAnnotations(),
                        Retention.InClassFile.INSTANCE));
        return new ParameterAnnotations(applied);
    }

    /** The value of an annotation's element, or of an element's default. */
    ElementValue value(final AnnotationValue value) {
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
                    : Optional.of(ClassFileTypes.INSTANCE.typeUse(ClassDesc.ofDescriptor(
                            literal.className().stringValue()))));
            case AnnotationValue.OfAnnotation nested -> new ElementValue.OfAnnotation(annotation(nested.annotation()));
            case AnnotationValue.OfArray array ->
                    new ElementValue.OfArray(new ElementValues(array.values().stream().map(this::value).toList()));
        };
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

    private ElementValue primitive(final PrimitiveType type, final Object constant) {
        return new ElementValue.OfPrimitive(type, String.valueOf(constant));
    }

    /** A class named by its descriptor, as an annotation's type or an enum's is: {@code Ljava/lang/Deprecated;}. */
    private ClassName className(final Utf8Entry descriptor) {
        final String text = descriptor.stringValue();
        return new ClassName(text.substring(1, text.length() - 1).replace('/', '.'));
    }
}
