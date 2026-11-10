package com.homing_labs.cq.rules.basic.enforce;

import com.homing_labs.cq.core.def.lang.JavaLang;
import com.homing_labs.cq.rules.basic.rule.NoStaticMethods;
import com.homing_labs.cq.enforcer.def.Enforcer;
import com.homing_labs.cq.enforcer.def.Findings;
import com.homing_labs.cq.enforcer.def.Result;
import com.homing_labs.cq.lang.java.def.JavaMethod;
import com.homing_labs.cq.lang.java.unit.JavaUnit;
import hue.captains.singapura.tao.ontology.StatelessFunctionalObject;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Checks {@link NoStaticMethods} on a Java unit: every static method declared in its top-level class or in a class
 * nested in it. What the compiler makes is not counted - lambda bodies and other synthetic methods, bridges, and an
 * enum's {@code values()} and {@code valueOf(String)}. Nor is {@code public static void main(String[])}, the one static
 * method the JVM asks for before Java 25, whose code cannot start any other way. Any other {@code main} is counted.
 *
 * <p>A class rule: the loaded class is enough, so a unit with or without its source will do.</p>
 */
public record NoStaticMethodsEnforcer()
        implements Enforcer<JavaLang, NoStaticMethods, JavaUnit>, StatelessFunctionalObject {

    public static final NoStaticMethodsEnforcer INSTANCE = new NoStaticMethodsEnforcer();

    @Override
    public Result<JavaLang, NoStaticMethods> check(final JavaUnit unit) {
        return new Result<>(new Findings<>(Arrays.stream(unit.type().getNestMembers())
                .flatMap(type -> Arrays.stream(type.getDeclaredMethods()))
                .filter(method -> Modifier.isStatic(method.getModifiers()))
                .filter(method -> !method.isSynthetic() && !method.isBridge() && !isEnumsOwn(method))
                .filter(method -> !isProgramsStart(method))
                .map(method -> new StaticMethod(new JavaMethod(method)))
                .collect(Collectors.toSet())));
    }

    private boolean isProgramsStart(final Method method) {
        return method.getName().equals("main") && Modifier.isPublic(method.getModifiers())
                && method.getReturnType() == void.class
                && Arrays.equals(method.getParameterTypes(), new Class<?>[]{String[].class});
    }

    private boolean isEnumsOwn(final Method method) {
        if (!method.getDeclaringClass().isEnum()) {
            return false;
        }
        final Class<?>[] parameters = method.getParameterTypes();
        return method.getName().equals("values") && parameters.length == 0
                || method.getName().equals("valueOf") && parameters.length == 1 && parameters[0] == String.class;
    }
}
