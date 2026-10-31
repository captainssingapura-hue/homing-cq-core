package com.homing_labs.cq.bench;

import hue.captains.singapura.tao.ontology.Stateless;

/**
 * How a class exists in its unit: as the unit's top-level class; as a member of the class it is declared in, static or
 * inner, as the taxonomy has them; or in the code of a method or an initialiser, local or anonymous - which the
 * taxonomy does not have yet.
 */
public sealed interface Existence extends Stateless {

    /** The unit's own class: a unit is a top-level class. */
    record TopLevel() implements Existence {

        public static final TopLevel INSTANCE = new TopLevel();
    }

    /** A static member class: the taxonomy's {@code StaticMemberClass}. */
    record StaticMember() implements Existence {

        public static final StaticMember INSTANCE = new StaticMember();
    }

    /** An inner class: the taxonomy's {@code MemberClass}. */
    record InnerMember() implements Existence {

        public static final InnerMember INSTANCE = new InnerMember();
    }

    /** A local class, declared in the code of a method or an initialiser. */
    record Local() implements Existence {

        public static final Local INSTANCE = new Local();
    }

    /** An anonymous class, made where an expression in some code makes it. */
    record Anonymous() implements Existence {

        public static final Anonymous INSTANCE = new Anonymous();
    }
}
