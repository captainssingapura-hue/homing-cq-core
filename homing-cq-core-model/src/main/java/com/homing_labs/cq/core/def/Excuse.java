package com.homing_labs.cq.core.def;

import hue.captains.singapura.tao.ontology.Stateless;

/**
 * Why a class may break a rule: a stateless thing of its own, typed to the rule it may excuse. Its reason is what it
 * is, said by its name and its documentation, never a string at the place it is pleaded. So excuses are a vocabulary:
 * one search finds every class pleading one, and deleting one fails to compile them all.
 *
 * @param <R> the rule it may excuse
 */
public interface Excuse<R extends Rule<?, ?>> extends Stateless {

}
