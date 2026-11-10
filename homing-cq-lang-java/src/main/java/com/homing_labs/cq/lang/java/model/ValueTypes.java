package com.homing_labs.cq.lang.java.model;

import com.homing_labs.cq.lang.java.taxonomy.AnonymousClass;
import com.homing_labs.cq.lang.java.taxonomy.ClassAnnotation;
import com.homing_labs.cq.lang.java.taxonomy.ClassKind;
import com.homing_labs.cq.lang.java.taxonomy.ClassProperty;
import com.homing_labs.cq.lang.java.taxonomy.Constructor;
import com.homing_labs.cq.lang.java.taxonomy.Dependency;
import com.homing_labs.cq.lang.java.taxonomy.EnumConstant;
import com.homing_labs.cq.lang.java.taxonomy.Field;
import com.homing_labs.cq.lang.java.taxonomy.LocalClass;
import com.homing_labs.cq.lang.java.taxonomy.MemberClass;
import com.homing_labs.cq.lang.java.taxonomy.Method;
import com.homing_labs.cq.lang.java.taxonomy.ObjectKind;
import com.homing_labs.cq.lang.java.taxonomy.RecordComponent;
import com.homing_labs.cq.lang.java.taxonomy.StaticField;
import com.homing_labs.cq.lang.java.taxonomy.StaticMemberClass;
import com.homing_labs.cq.lang.java.taxonomy.StaticMethod;
import com.homing_labs.cq.lang.java.taxonomy.Superclass;
import com.homing_labs.cq.lang.java.taxonomy.Superinterface;
import com.homing_labs.cq.lang.java.taxonomy.TypeParameter;
import hue.captains.singapura.tao.ontology.Scalar;
import hue.captains.singapura.tao.ontology.StatelessFunctionalObject;

/**
 * What one of each object of the taxonomy holds. The taxonomy says which objects a kind of class has and how many; this
 * says what each one is, as a value. The switch is over the whole taxonomy, so an object added to it is not compiled
 * until it is given a value here.
 */
public record ValueTypes() implements StatelessFunctionalObject {

    public static final ValueTypes INSTANCE = new ValueTypes();

    /** The type of the values of an object. A kind of class holds none: it is no member of a class. */
    public Class<? extends Scalar> of(final ObjectKind object) {
        return switch (object) {
            case ClassKind kind -> throw new IllegalArgumentException(kind.getClass().getSimpleName()
                    + " is a kind of class, and no member of one");
            case ClassProperty.Visibility _ -> Visibility.class;
            case ClassProperty.Extensibility _ -> Extensibility.class;
            case ClassAnnotation _ -> AppliedAnnotation.class;
            case TypeParameter _ -> TypeParameterValue.class;
            case Superclass _, Superinterface _ -> TypeUse.class;
            case Dependency _ -> ClassName.class;
            case Constructor _ -> ConstructorValue.class;
            case StaticField _, Field _ -> FieldValue.class;
            case EnumConstant _ -> EnumConstantValue.class;
            case RecordComponent _ -> RecordComponentValue.class;
            case StaticMethod _, Method _ -> MethodValue.class;
            case StaticMemberClass _, MemberClass _ -> NestedClass.class;
            case LocalClass _, AnonymousClass _ -> EnclosedClass.class;
        };
    }
}
