package com.homing_labs.cq.rules.basic;

import com.homing_labs.cq.scan.Declaration;
import com.homing_labs.cq.scan.Ruleset;
import hue.captains.singapura.tao.ontology.StatelessFunctionalObject;

import java.util.List;

/**
 * All the basic rules, one ruleset for each principle. A project opts in by depending on this module and composing this
 * declaration with the code it checks, for the bench and a build's gate alike:
 * {@code ShopCode.INSTANCE.and(BasicRules.INSTANCE)}. For only some principles, it composes their rulesets instead,
 * such as {@link DivideAndConquerRules}; for only some rules, it lists their enforcers in a ruleset of its own.
 */
public record BasicRules() implements Declaration, StatelessFunctionalObject {

    public static final BasicRules INSTANCE = new BasicRules();

    @Override
    public List<Ruleset> rulesets() {
        return List.of(BehaviourLivesOnObjectsRules.INSTANCE, NoGlobalMutableStateRules.INSTANCE,
                EncapsulationRules.INSTANCE, DivideAndConquerRules.INSTANCE);
    }
}
