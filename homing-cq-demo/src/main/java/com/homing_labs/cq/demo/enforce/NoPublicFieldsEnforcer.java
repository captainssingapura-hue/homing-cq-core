package com.homing_labs.cq.demo.enforce;

import com.homing_labs.cq.core.def.lang.JavaLang;
import com.homing_labs.cq.demo.rule.NoPublicFields;
import com.homing_labs.cq.enforcer.def.Enforcer;
import com.homing_labs.cq.enforcer.def.Result;
import com.homing_labs.cq.lang.java.unit.ClassUnit;
import com.homing_labs.cq.lang.java.unit.JavaNestedUnit;
import com.homing_labs.cq.lang.java.unit.JavaNests;
import hue.captains.singapura.tao.ontology.StatelessFunctionalObject;

/**
 * Checks {@link NoPublicFields} on a concrete class, from the attributes its unit holds: the class itself, then each
 * class nested in it, delegated to {@link NoPublicFieldsInNestedClassEnforcer}, with every finding on this unit.
 */
public record NoPublicFieldsEnforcer()
        implements Enforcer<JavaLang, NoPublicFields, ClassUnit>, StatelessFunctionalObject {

    public static final NoPublicFieldsEnforcer INSTANCE = new NoPublicFieldsEnforcer();

    @Override
    public Result<JavaLang, NoPublicFields> check(final ClassUnit unit) {
        Result<JavaLang, NoPublicFields> result = new Result<>(
                PublicFields.INSTANCE.in(unit.identity().name(), unit.attributes()));
        for (final JavaNestedUnit nested : JavaNests.INSTANCE.of(unit)) {
            result = result.and(NoPublicFieldsInNestedClassEnforcer.INSTANCE.check(nested));
        }
        return result;
    }
}
