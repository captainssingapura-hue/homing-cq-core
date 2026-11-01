package com.homing_labs.cq.demo.enforce;

import com.homing_labs.cq.core.def.lang.JavaLang;
import com.homing_labs.cq.demo.rule.TopLevelClassesWithin250Lines;
import com.homing_labs.cq.enforcer.def.Finding;
import com.homing_labs.cq.lang.java.model.ClassName;

/**
 * A top-level class written in more effective lines than the rule allows: the class, and how many it is written in.
 */
public record LongClass(ClassName name, int effectiveLines)
        implements Finding<JavaLang, TopLevelClassesWithin250Lines> {
}
