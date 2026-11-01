package com.homing_labs.cq.demo.enforce;

import com.homing_labs.cq.core.def.lang.JavaLang;
import com.homing_labs.cq.demo.rule.MethodsWithin50Lines;
import com.homing_labs.cq.enforcer.def.Finding;
import com.homing_labs.cq.lang.java.model.ClassName;

/**
 * A method or a constructor written in more effective lines than the rule allows: the class it is in, the member by
 * its name and its parameters as written - a constructor by its class's name - the line it is written from, and how
 * many effective lines it is written in.
 */
public record LongMethod(ClassName in, String member, int line, int effectiveLines)
        implements Finding<JavaLang, MethodsWithin50Lines> {
}
