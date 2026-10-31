package com.homing_labs.cq.demo.enforce;

import com.homing_labs.cq.core.def.lang.JavaLang;
import com.homing_labs.cq.demo.rule.NoStaticMutableVariables;
import com.homing_labs.cq.enforcer.def.Enforcer;
import com.homing_labs.cq.enforcer.def.Findings;
import com.homing_labs.cq.enforcer.def.Result;
import com.homing_labs.cq.lang.java.def.JavaField;
import com.homing_labs.cq.lang.java.unit.JavaUnit;
import hue.captains.singapura.tao.ontology.Immutable;
import hue.captains.singapura.tao.ontology.StatelessFunctionalObject;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.net.URL;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Period;
import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Checks {@link NoStaticMutableVariables} on a Java unit: every static field declared in its top-level class or in a
 * class nested in it. A field that is not final can change; a final one can still change what it holds unless its
 * type is known to be immutable - a primitive, an enum, a type marked {@link Immutable} (every jOntology marker but
 * {@code Mutable}), or one of the JDK types jOntology's enforcer takes as immutable. An array never is. Synthetic
 * fields, which the compiler makes, are not counted.
 *
 * <p>A class rule: the loaded class is enough, and it is never initialised - a field's declared type is judged, not
 * the value it holds.</p>
 */
public record NoStaticMutableVariablesEnforcer()
        implements Enforcer<JavaLang, NoStaticMutableVariables, JavaUnit>, StatelessFunctionalObject {

    public static final NoStaticMutableVariablesEnforcer INSTANCE = new NoStaticMutableVariablesEnforcer();

    @Override
    public Result<JavaLang, NoStaticMutableVariables> check(final JavaUnit unit) {
        return new Result<>(new Findings<>(Arrays.stream(unit.type().getNestMembers())
                .flatMap(type -> Arrays.stream(type.getDeclaredFields()))
                .filter(field -> Modifier.isStatic(field.getModifiers()) && !field.isSynthetic())
                .filter(field -> !Modifier.isFinal(field.getModifiers()) || !knownImmutable(field.getType()))
                .map(this::finding)
                .collect(Collectors.toSet())));
    }

    private boolean knownImmutable(final Class<?> type) {
        return type.isPrimitive()
                || type.isEnum()
                || Immutable.class.isAssignableFrom(type)
                || Set.of(String.class, Boolean.class, Byte.class, Short.class, Integer.class, Long.class,
                        Float.class, Double.class, Character.class, BigDecimal.class, BigInteger.class, UUID.class,
                        LocalDate.class, LocalTime.class, LocalDateTime.class, Instant.class, Duration.class,
                        Period.class, URI.class, URL.class, Pattern.class, Class.class, Optional.class, Void.class)
                .contains(type);
    }

    private StaticMutableVariable finding(final Field field) {
        return Modifier.isFinal(field.getModifiers())
                ? new StaticMutableVariable.NotKnownImmutable(new JavaField(field))
                : new StaticMutableVariable.NotFinal(new JavaField(field));
    }
}
