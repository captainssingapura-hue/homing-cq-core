package com.homing_labs.cq.scan;

import com.homing_labs.cq.enforcer.def.Enforcer;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Comparator;
import java.util.List;

/**
 * The ruleset of the enforcers under a package, named by the package: every concrete top-level class there that
 * implements {@link Enforcer} - its public static {@code INSTANCE}, or else one made with its public constructor that
 * takes nothing. The package is read wherever the class loader has it, test code included. Finding an enforcer
 * initialises its class.
 */
public record PackageRuleset(String rootPackage) implements Ruleset {

    @Override
    public String name() {
        return rootPackage;
    }

    /** The enforcers under the package, as {@code loader} finds them, by class name. */
    @Override
    public List<Enforcer<?, ?, ?>> enforcers(final ClassLoader loader) {
        return ClasspathUnits.INSTANCE.read(rootPackage, loader).stream()
                .map(unit -> unit.type())
                .filter(type -> Enforcer.class.isAssignableFrom(type) && !type.isInterface()
                        && !Modifier.isAbstract(type.getModifiers()))
                .sorted(Comparator.comparing(Class::getName))
                .<Enforcer<?, ?, ?>>map(this::instance)
                .toList();
    }

    private Enforcer<?, ?, ?> instance(final Class<?> type) {
        try {
            final Field instance = type.getField("INSTANCE");
            if (Modifier.isStatic(instance.getModifiers()) && type.isAssignableFrom(instance.getType())) {
                return (Enforcer<?, ?, ?>) instance.get(null);
            }
        } catch (final NoSuchFieldException | IllegalAccessException noInstance) {
            // made with its constructor instead
        }
        try {
            return (Enforcer<?, ?, ?>) type.getConstructor().newInstance();
        } catch (final ReflectiveOperationException e) {
            throw new IllegalStateException(type.getName() + " has neither a public static INSTANCE nor a public"
                    + " constructor that takes nothing", e);
        }
    }
}
