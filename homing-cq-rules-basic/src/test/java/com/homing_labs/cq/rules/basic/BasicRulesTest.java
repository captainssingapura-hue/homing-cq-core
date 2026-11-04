package com.homing_labs.cq.rules.basic;

import com.homing_labs.cq.enforcer.def.Enforcer;
import com.homing_labs.cq.enforcer.def.EnforcerSignature;
import com.homing_labs.cq.enforcer.def.Enforcers;
import com.homing_labs.cq.rules.basic.principle.BehaviourLivesOnObjects;
import com.homing_labs.cq.rules.basic.principle.DivideAndConquer;
import com.homing_labs.cq.rules.basic.principle.Encapsulation;
import com.homing_labs.cq.rules.basic.principle.NoGlobalMutableState;
import com.homing_labs.cq.rules.basic.rule.MethodsWithin50Lines;
import com.homing_labs.cq.rules.basic.rule.NoPublicFields;
import com.homing_labs.cq.rules.basic.rule.NoStaticMethods;
import com.homing_labs.cq.rules.basic.rule.NoStaticMutableVariables;
import com.homing_labs.cq.rules.basic.rule.TopLevelClassesWithin250Lines;
import com.homing_labs.cq.scan.Declaration;
import com.homing_labs.cq.scan.NamedDeclarations;
import com.homing_labs.cq.scan.PackageRuleset;
import com.homing_labs.cq.scan.Ruleset;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * How a project opts in: all the rules, the rules of a principle, or its own pick, each a declaration it composes.
 */
class BasicRulesTest {

    private static final String SOURCES = "/META-INF/homing-cq/sources/";

    private final ClassLoader loader = getClass().getClassLoader();

    @Test
    void theRulesetsHoldEveryEnforcerOfTheModule() {
        assertEquals(enforcersOf(List.of(new PackageRuleset("com.homing_labs.cq.rules.basic"))),
                enforcersOf(BasicRules.INSTANCE.rulesets()));
    }

    @Test
    void eachRulesetHoldsTheRulesOfOnePrinciple() {
        assertEquals(Map.of(
                        BehaviourLivesOnObjectsRules.INSTANCE, Map.of(
                                BehaviourLivesOnObjects.class, Set.of(NoStaticMethods.class)),
                        NoGlobalMutableStateRules.INSTANCE, Map.of(
                                NoGlobalMutableState.class, Set.of(NoStaticMutableVariables.class)),
                        EncapsulationRules.INSTANCE, Map.of(
                                Encapsulation.class, Set.of(NoPublicFields.class)),
                        DivideAndConquerRules.INSTANCE, Map.of(
                                DivideAndConquer.class,
                                Set.of(TopLevelClassesWithin250Lines.class, MethodsWithin50Lines.class))),
                BasicRules.INSTANCE.rulesets().stream().collect(Collectors.toMap(ruleset -> ruleset, this::principles)));
    }

    @Test
    void aRulesetDeclaresItselfAloneAndNamesThePrinciple() {
        final Map<String, List<Ruleset>> declared = new LinkedHashMap<>();
        BasicRules.INSTANCE.rulesets().forEach(ruleset ->
                declared.put(ruleset.name(), ((Declaration) ruleset).rulesets()));
        assertEquals(Map.of(
                        "behaviour lives on objects", List.of(BehaviourLivesOnObjectsRules.INSTANCE),
                        "no global mutable state", List.of(NoGlobalMutableStateRules.INSTANCE),
                        "encapsulation", List.of(EncapsulationRules.INSTANCE),
                        "divide and conquer", List.of(DivideAndConquerRules.INSTANCE)),
                declared);
    }

    @Test
    void aProjectComposesWhatItOptsInToAndARulesetNamedTwiceCountsOnce() {
        final Declaration shop = new Declaration() {
            @Override
            public List<String> packages() {
                return List.of("shop");
            }
        };
        final Declaration sizes = shop.and(DivideAndConquerRules.INSTANCE);
        assertEquals(List.of("shop"), sizes.packages());
        assertEquals(List.of(DivideAndConquerRules.INSTANCE), sizes.rulesets());
        assertEquals(List.of(DivideAndConquerRules.INSTANCE, BehaviourLivesOnObjectsRules.INSTANCE,
                        NoGlobalMutableStateRules.INSTANCE, EncapsulationRules.INSTANCE),
                sizes.and(BasicRules.INSTANCE).rulesets());
    }

    @Test
    void aProjectOptsInByNameWhereNoCodeCanHandTheRulesOver() {
        assertEquals(BasicRules.INSTANCE.rulesets(),
                NamedDeclarations.INSTANCE.of(List.of(BasicRules.class.getName()), loader).rulesets());
        assertEquals(List.of(EncapsulationRules.INSTANCE),
                NamedDeclarations.INSTANCE.of(List.of(EncapsulationRules.class.getName()), loader).rulesets());
    }

    @Test
    void theModulesClassesComeWithTheirSourcesToExplainThem() {
        final List<Class<?>> explained = BasicRules.INSTANCE.rulesets().stream()
                .flatMap(ruleset -> ruleset.enforcers(loader).stream())
                .flatMap(enforcer -> {
                    final EnforcerSignature signature = Enforcers.INSTANCE.signatureOf(enforcer);
                    return Stream.of(signature.principle(), signature.rule(), enforcer.getClass());
                })
                .distinct().toList();
        assertEquals(15, explained.size(), "the 4 principles, the 5 rules, and each enforcer: "
                + explained.stream().map(Class::getSimpleName).toList());
        explained.forEach(type -> assertTrue(source(type).contains("public record " + type.getSimpleName()),
                type.getName()));
    }

    private Set<Enforcer<?, ?, ?>> enforcersOf(final List<Ruleset> rulesets) {
        return rulesets.stream().flatMap(ruleset -> ruleset.enforcers(loader).stream()).collect(Collectors.toSet());
    }

    /** The rules a ruleset checks, by the principle each serves. */
    private Map<Class<?>, Set<Class<?>>> principles(final Ruleset ruleset) {
        return ruleset.enforcers(loader).stream().map(Enforcers.INSTANCE::signatureOf).collect(Collectors.groupingBy(
                EnforcerSignature::principle, Collectors.mapping(EnforcerSignature::rule, Collectors.toSet())));
    }

    /** The source packed with the class. */
    private String source(final Class<?> type) {
        try (InputStream in = getClass().getResourceAsStream(SOURCES + type.getName().replace('.', '/') + ".java")) {
            return in == null ? "" : new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
