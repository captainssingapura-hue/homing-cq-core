package com.homing_labs.cq.demo.enforce;

import com.homing_labs.cq.core.def.lang.JavaLang;
import com.homing_labs.cq.demo.rule.NoPublicFields;
import com.homing_labs.cq.enforcer.def.Finding;
import com.homing_labs.cq.lang.java.model.ClassName;

/**
 * A public instance field, in the class that declares it.
 */
public record PublicField(ClassName in, String name) implements Finding<JavaLang, NoPublicFields> {
}
