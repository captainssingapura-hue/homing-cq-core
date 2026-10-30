package com.homing_labs.cq.demo.taxonomy;

import com.homing_labs.cq.lang.java.taxonomy.Cardinality;
import com.homing_labs.cq.lang.java.taxonomy.MemberOf;
import com.homing_labs.cq.lang.java.taxonomy.ObjectKind;
import com.homing_labs.cq.lang.java.taxonomy.Subordinate;
import com.homing_labs.cq.lang.java.taxonomy.scan.ClassScanner;
import com.homing_labs.cq.lang.java.taxonomy.scan.Occurrence;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Every class in the samples' nests scanned, and what was found aggregated by the object of the taxonomy it is. Each
 * object occurs at least once, and in every place the taxonomy gives it; nothing occurs where the taxonomy gives it no
 * place, and no class has more or fewer of an object than the taxonomy allows.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TheSamplesCoverTheTaxonomyTest {

    private final List<Occurrence> found = scan(SampleClass.class, SampleAbstractClass.class, SampleInterface.class,
            SampleEnum.class, SampleRecord.class, SampleAnnotation.class);

    /** What was found, by the object of the taxonomy it is. */
    private final Map<Class<?>, List<Occurrence>> byObject =
            found.stream().collect(Collectors.groupingBy(occurrence -> occurrence.kind().getClass()));

    @ParameterizedTest(name = "{0}")
    @MethodSource("objects")
    void everyObjectOccurs(final Class<?> object) {
        assertFalse(byObject.getOrDefault(object, List.of()).isEmpty(), object.getSimpleName() + " never occurs");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("placements")
    void everyObjectOccursInEveryPlaceItHas(final Placement placement) {
        assertFalse(members().filter(placement::holds).toList().isEmpty(), placement + " never occurs");
    }

    @Test
    void nothingOccursWhereTheTaxonomyGivesItNoPlace() {
        assertEquals(List.of(), members().filter(member -> !member.place().isInstance(member.kind())).toList());
    }

    @Test
    void noClassHasMoreOrFewerOfAnObjectThanTheTaxonomyAllows() {
        final List<String> wrong = new ArrayList<>();
        members().collect(Collectors.groupingBy(Occurrence.OfMember::inClass)).forEach((inClass, members) -> {
            final Class<?> top = members.stream()
                    .filter(member -> member.kind() instanceof Subordinate.ClassObject)
                    .findFirst().orElseThrow().place();
            placements().map(Named::getPayload)
                    .filter(placement -> placement.place() == top || placement.place().getEnclosingClass() == top)
                    .forEach(placement -> {
                        final long count = members.stream().filter(placement::holds).count();
                        if (!placement.allows(count)) {
                            wrong.add(inClass + " has " + count + " of " + placement);
                        }
                    });
        });
        assertEquals(List.of(), wrong);
    }

    /** Every object of the taxonomy. */
    Stream<Named<Class<?>>> objects() {
        return leaves(ObjectKind.class).stream().map(object -> Named.of(object.getSimpleName(), object));
    }

    /** Every place each object has in the taxonomy, with how many of it a class has there. */
    Stream<Named<Placement>> placements() {
        return leaves(ObjectKind.class).stream()
                .flatMap(object -> Arrays.stream(object.getGenericInterfaces())
                        .flatMap(type -> type instanceof ParameterizedType parameterized
                                && MemberOf.class.isAssignableFrom((Class<?>) parameterized.getRawType())
                                ? Stream.of(new Placement((Class<?>) parameterized.getRawType(), object,
                                (Class<?>) parameterized.getActualTypeArguments()[0]))
                                : Stream.empty()))
                .map(placement -> Named.of(placement.toString(), placement));
    }

    private Stream<Occurrence.OfMember> members() {
        return found.stream()
                .flatMap(occurrence -> occurrence instanceof Occurrence.OfMember member ? Stream.of(member)
                        : Stream.empty());
    }

    /** The types a sealed type permits, and those they permit in turn, down to the ones that permit none. */
    private List<Class<?>> leaves(final Class<?> type) {
        if (!type.isSealed()) {
            return List.of(type);
        }
        return Arrays.stream(type.getPermittedSubclasses())
                .flatMap(permitted -> leaves(permitted).stream())
                .sorted(Comparator.comparing(Class::getName))
                .toList();
    }

    /** Every class in the samples' nests, scanned from its class file. */
    private List<Occurrence> scan(final Class<?>... samples) {
        return Arrays.stream(samples)
                .flatMap(sample -> Arrays.stream(sample.getNestMembers()))
                .flatMap(type -> ClassScanner.INSTANCE.scan(classFile(type)).stream())
                .toList();
    }

    private byte[] classFile(final Class<?> type) {
        try (InputStream in = type.getClassLoader().getResourceAsStream(type.getName().replace('.', '/') + ".class")) {
            return in.readAllBytes();
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** A place an object has in the taxonomy, and how many of it a class has there. */
    record Placement(Class<?> place, Class<?> object, Class<?> cardinality) {

        boolean holds(final Occurrence.OfMember member) {
            return member.place() == place && member.kind().getClass() == object;
        }

        boolean allows(final long count) {
            if (cardinality == Cardinality.One.class) {
                return count == 1;
            }
            if (cardinality == Cardinality.ZeroOrOne.class) {
                return count <= 1;
            }
            return cardinality != Cardinality.OneOrMore.class || count >= 1;
        }

        @Override
        public String toString() {
            final String name = place.getName();
            return object.getSimpleName() + " in " + name.substring(name.indexOf('$') + 1).replace('$', '.')
                    + " (" + cardinality.getSimpleName() + ")";
        }
    }
}
