package com.homing_labs.cq.rules.basic.enforce;

import com.homing_labs.cq.core.def.lang.JavaLang;
import com.homing_labs.cq.rules.basic.rule.NoStaticMutableVariables;
import com.homing_labs.cq.enforcer.def.Finding;
import com.homing_labs.cq.lang.java.def.JavaField;

/**
 * A static field {@link NoStaticMutableVariables} found, told apart by why it can change.
 */
public sealed interface StaticMutableVariable extends Finding<JavaLang, NoStaticMutableVariables> {

    /** The field. */
    JavaField field();

    /** A static field that is not final: anyone who can reach it can set it. */
    record NotFinal(JavaField field) implements StaticMutableVariable {
    }

    /** A static final field whose type is not known to be immutable: what it holds can still change. */
    record NotKnownImmutable(JavaField field) implements StaticMutableVariable {
    }
}
