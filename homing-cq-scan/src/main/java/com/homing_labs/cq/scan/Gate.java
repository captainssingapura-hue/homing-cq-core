package com.homing_labs.cq.scan;

import hue.captains.singapura.tao.ontology.StatelessFunctionalObject;

import java.util.List;
import java.util.stream.Stream;

/**
 * A build's gate: scans by a declaration, and lists what keeps the code from passing - each rule a unit breaks, or
 * could not be checked by; each unit read without its source, which a rule on sources would pass by checking
 * nothing; and a scan that read no unit, or had no rule to check by, which would pass by checking nothing at all. A
 * build runs it as a test, or as a program whose arguments name the declarations to compose.
 */
public record Gate() implements StatelessFunctionalObject {

    public static final Gate INSTANCE = new Gate();

    /** What keeps the code {@code declaration} covers from passing, one line each: none, when it passes. */
    public List<String> failures(final Declaration declaration, final ClassLoader loader) {
        return failures(Scanner.INSTANCE.scan(declaration, loader));
    }

    /** What keeps the code a scan read from passing, one line each: none, when it passes. */
    public List<String> failures(final ScanResult result) {
        return Stream.concat(nothingChecked(result).stream(), result.units().items().stream().flatMap(this::failures))
                .toList();
    }

    /** Why a scan checked nothing, one line each: none, when it read units and had rules to check them by. */
    public List<String> nothingChecked(final ScanResult result) {
        return Stream.concat(
                result.units().items().isEmpty() ? Stream.of("no unit read: nothing was checked") : Stream.empty(),
                result.rules() == 0 ? Stream.of("no rule to check by: nothing was checked") : Stream.empty()).toList();
    }

    /** How a scan came out, in one line: the units, the rules, and how many failures, or that it passed. */
    public String closing(final ScanResult result) {
        final int failures = failures(result).size();
        return result.units().items().size() + " units by " + result.rules() + " rules: "
                + (failures == 0 ? "passed" : failures + " failures");
    }

    /**
     * The gate as a program: scans by the declarations its arguments name, composed in order, prints each failure,
     * and fails, by throwing, when there is any. It is an instance method: the launcher makes the gate, then runs it.
     */
    public void main(final String[] args) {
        final ClassLoader loader = getClass().getClassLoader();
        final ScanResult result = Scanner.INSTANCE.scan(NamedDeclarations.INSTANCE.of(List.of(args), loader), loader);
        final List<String> failures = failures(result);
        failures.forEach(System.out::println);
        if (!failures.isEmpty()) {
            throw new IllegalStateException(closing(result));
        }
        System.out.println(closing(result));
    }

    private Stream<String> failures(final ScannedUnit scanned) {
        final String unit = scanned.unit().type().getName();
        final Stream<String> unsourced = scanned.unit().source().isPresent() ? Stream.empty()
                : Stream.of(unit + ": read without its source");
        return Stream.concat(unsourced, scanned.applied().items().stream()
                .filter(applied -> !applied.complies())
                .map(applied -> unit + " " + applied.signature().rule().getSimpleName() + ": "
                        + String.join("; ", applied.lines())));
    }
}
