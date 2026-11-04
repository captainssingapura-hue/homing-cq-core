package com.homing_labs.cq.rules.basic.enforce;

import com.homing_labs.cq.core.def.lang.JavaLang;
import com.homing_labs.cq.rules.basic.rule.NoPublicFields;
import com.homing_labs.cq.enforcer.def.Enforcer;
import com.homing_labs.cq.enforcer.def.Result;
import com.homing_labs.cq.lang.java.unit.JavaNestedUnit;
import com.homing_labs.cq.lang.java.unit.JavaNests;
import com.homing_labs.cq.lang.java.unit.JavaUnit;
import hue.captains.singapura.tao.ontology.StatelessFunctionalObject;

/**
 * Checks {@link NoPublicFields} on a unit of any kind, from the attributes it holds: the top-level class itself, then
 * each class nested in it, delegated to {@link NoPublicFieldsInNestedClassEnforcer}, with every finding on this unit.
 * A record, an interface and an annotation have no public instance field of their own, but a class nested in one can.
 */
public record NoPublicFieldsEnforcer()
        implements Enforcer<JavaLang, NoPublicFields, JavaUnit>, StatelessFunctionalObject {

    public static final NoPublicFieldsEnforcer INSTANCE = new NoPublicFieldsEnforcer();

    @Override
    public Result<JavaLang, NoPublicFields> check(final JavaUnit unit) {
        Result<JavaLang, NoPublicFields> result = new Result<>(
                PublicFields.INSTANCE.in(unit.identity().name(), unit.attributes()));
        for (final JavaNestedUnit nested : JavaNests.INSTANCE.of(unit)) {
            result = result.and(NoPublicFieldsInNestedClassEnforcer.INSTANCE.check(nested));
        }
        return result;
    }
}
