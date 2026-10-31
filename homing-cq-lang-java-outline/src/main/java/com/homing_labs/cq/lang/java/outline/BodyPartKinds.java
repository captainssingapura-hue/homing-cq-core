package com.homing_labs.cq.lang.java.outline;

import com.homing_labs.cq.lang.java.taxonomy.Constructor;
import com.homing_labs.cq.lang.java.taxonomy.EnumConstant;
import com.homing_labs.cq.lang.java.taxonomy.Field;
import com.homing_labs.cq.lang.java.taxonomy.MemberClass;
import com.homing_labs.cq.lang.java.taxonomy.Method;
import com.homing_labs.cq.lang.java.taxonomy.ObjectKind;
import com.homing_labs.cq.lang.java.taxonomy.StaticField;
import com.homing_labs.cq.lang.java.taxonomy.StaticInitialiser;
import com.homing_labs.cq.lang.java.taxonomy.StaticMemberClass;
import com.homing_labs.cq.lang.java.taxonomy.StaticMethod;
import hue.captains.singapura.tao.ontology.StatelessFunctionalObject;

import java.util.Optional;

/**
 * Links each part of a type's body to the kind of object the taxonomy has for it, in the class the type compiles to.
 *
 * <p>What the part does not say, the type it is in does: an interface's fields are static, its member types too, and
 * its methods abstract unless written default, static or private. Two parts have no kind: an instance initializer,
 * whose code runs as part of each constructor's, and a semicolon.</p>
 */
public record BodyPartKinds() implements StatelessFunctionalObject {

    public static final BodyPartKinds INSTANCE = new BodyPartKinds();

    /** The kind of object the part declares, in a type declared with the keyword. */
    public Optional<ObjectKind> of(final TypeKeyword enclosing, final BodyPart part) {
        final boolean inInterface = enclosing instanceof TypeKeyword.Interface
                || enclosing instanceof TypeKeyword.Annotation;
        return switch (part) {
            case BodyPart.Field field -> Optional.of(inInterface || field.modifiers().has("static")
                    ? StaticField.INSTANCE : Field.INSTANCE);
            case BodyPart.Method method -> Optional.of(method(enclosing, method.modifiers()));
            case BodyPart.Constructor _, BodyPart.CompactConstructor _ -> Optional.of(Constructor.INSTANCE);
            case BodyPart.Initializer initializer -> initializer.isStatic()
                    ? Optional.of(StaticInitialiser.INSTANCE) : Optional.empty();
            case BodyPart.TypeDeclaration type -> Optional.of(inInterface || type.header().modifiers().has("static")
                    || !(type.header().keyword() instanceof TypeKeyword.Class)
                    ? StaticMemberClass.INSTANCE : MemberClass.INSTANCE);
            case BodyPart.EnumConstant _ -> Optional.of(EnumConstant.INSTANCE);
            case BodyPart.EmptyDeclaration _, BodyPart.Unrecognised _ -> Optional.empty();
        };
    }

    private ObjectKind method(final TypeKeyword enclosing, final Modifiers modifiers) {
        if (modifiers.has("static")) {
            return modifiers.has("native") ? StaticMethod.NativeImplementation.INSTANCE
                    : StaticMethod.Implementation.INSTANCE;
        }
        return switch (enclosing) {
            case TypeKeyword.Annotation _ -> Method.Declaration.INSTANCE;
            case TypeKeyword.Interface _ -> modifiers.has("default") ? Method.DefaultImplementation.INSTANCE
                    : modifiers.has("private") ? Method.Implementation.INSTANCE : Method.Declaration.INSTANCE;
            case TypeKeyword.Class _, TypeKeyword.Enum _, TypeKeyword.Record _ -> modifiers.has("abstract")
                    ? Method.Declaration.INSTANCE : modifiers.has("native") ? Method.NativeImplementation.INSTANCE
                    : Method.Implementation.INSTANCE;
        };
    }
}
