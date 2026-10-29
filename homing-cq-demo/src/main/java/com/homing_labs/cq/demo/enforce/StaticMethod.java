package com.homing_labs.cq.demo.enforce;

import com.homing_labs.cq.core.def.lang.JavaLang;
import com.homing_labs.cq.demo.rule.NoStaticMethods;
import com.homing_labs.cq.enforcer.def.Finding;
import com.homing_labs.cq.lang.java.def.JavaMethod;

/**
 * A static method {@link NoStaticMethods} found.
 */
public record StaticMethod(JavaMethod method) implements Finding<JavaLang, NoStaticMethods> {
}
