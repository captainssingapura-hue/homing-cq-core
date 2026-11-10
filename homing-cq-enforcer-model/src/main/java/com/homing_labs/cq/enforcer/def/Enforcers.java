package com.homing_labs.cq.enforcer.def;

import com.homing_labs.cq.core.def.QcUnit;
import com.homing_labs.cq.core.def.Rule;
import hue.captains.singapura.tao.ontology.StatelessFunctionalObject;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Reads what an enforcer is for from its class, and runs it on the units it checks. An enforcer's class names the
 * language, rule and unit it is for as it implements {@link Enforcer}, and its rule's class names the principle as it
 * implements {@link Rule}; so a unit is checked by every enforcer whose unit type it is of - by one for records, say,
 * only if it is a record.
 */
public record Enforcers() implements StatelessFunctionalObject {

    public static final Enforcers INSTANCE = new Enforcers();

    /** What an enforcer is for. Its class, or a superclass, must name the type arguments of {@link Enforcer}. */
    public EnforcerSignature signatureOf(final Enforcer<?, ?, ?> enforcer) {
        final List<Class<?>> enforced = arguments(enforcer.getClass(), Enforcer.class);
        final List<Class<?>> ruled = arguments(enforced.get(1), Rule.class);
        return new EnforcerSignature(enforced.get(0), ruled.get(0), enforced.get(1), enforced.get(2));
    }

    /** The result of an enforcer on a unit it checks. */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public Result<?, ?> check(final Enforcer<?, ?, ?> enforcer, final QcUnit<?> unit) {
        final EnforcerSignature signature = signatureOf(enforcer);
        if (!signature.checks(unit)) {
            throw new IllegalArgumentException(enforcer.getClass().getName() + " checks units of type "
                    + signature.unit().getName() + ", and " + unit + " is not one");
        }
        return ((Enforcer) enforcer).check(unit);
    }

    /** The classes a type gives a generic interface as type arguments, found on the type or a superclass of it. */
    private List<Class<?>> arguments(final Class<?> type, final Class<?> generic) {
        for (Class<?> at = type; at != null; at = at.getSuperclass()) {
            for (final Type implemented : at.getGenericInterfaces()) {
                if (implemented instanceof ParameterizedType parameterized && parameterized.getRawType() == generic) {
                    final List<Optional<Class<?>>> arguments = Arrays.stream(parameterized.getActualTypeArguments())
                            .map(this::classOf)
                            .toList();
                    if (arguments.stream().allMatch(Optional::isPresent)) {
                        return arguments.stream().<Class<?>>map(Optional::orElseThrow).toList();
                    }
                }
            }
        }
        throw new IllegalArgumentException(type.getName() + " does not name the type arguments it gives "
                + generic.getSimpleName() + ", so what it is for cannot be read");
    }

    private Optional<Class<?>> classOf(final Type argument) {
        return switch (argument) {
            case Class<?> named -> Optional.of(named);
            case ParameterizedType parameterized -> classOf(parameterized.getRawType());
            default -> Optional.empty();
        };
    }
}
