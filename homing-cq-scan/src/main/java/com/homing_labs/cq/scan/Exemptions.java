package com.homing_labs.cq.scan;

import hue.captains.singapura.tao.ontology.StatelessFunctionalObject;

import java.util.Set;

/**
 * What one class is let off: a stateless object of its own for each class exempted, named for that class by its type.
 * The code that checks declares it, in a {@link Declaration}, and the class let off knows nothing of it, as it knows
 * nothing of the check.
 *
 * @param <C> the class let off, which every exemption here must be for
 */
public interface Exemptions<C> extends StatelessFunctionalObject {

    /** The rules the class is let off, each with the excuse it pleads. */
    Set<Exemption<C, ?, ?>> exemptions();
}
