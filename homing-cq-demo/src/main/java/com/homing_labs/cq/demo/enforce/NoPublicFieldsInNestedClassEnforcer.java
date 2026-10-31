package com.homing_labs.cq.demo.enforce;

import com.homing_labs.cq.core.def.lang.JavaLang;
import com.homing_labs.cq.demo.rule.NoPublicFields;
import com.homing_labs.cq.enforcer.def.Enforcer;
import com.homing_labs.cq.enforcer.def.Result;
import com.homing_labs.cq.lang.java.unit.JavaNestedUnit;
import hue.captains.singapura.tao.ontology.StatelessFunctionalObject;

/**
 * {@link NoPublicFields} on a class nested in a unit, static or inner: the sub-rule {@link NoPublicFieldsEnforcer}
 * delegates to, one nested class at a time.
 */
public record NoPublicFieldsInNestedClassEnforcer()
        implements Enforcer<JavaLang, NoPublicFields, JavaNestedUnit>, StatelessFunctionalObject {

    public static final NoPublicFieldsInNestedClassEnforcer INSTANCE = new NoPublicFieldsInNestedClassEnforcer();

    @Override
    public Result<JavaLang, NoPublicFields> check(final JavaNestedUnit unit) {
        return new Result<>(PublicFields.INSTANCE.in(unit.nested().object().name(),
                unit.nested().object().attributes()));
    }
}
