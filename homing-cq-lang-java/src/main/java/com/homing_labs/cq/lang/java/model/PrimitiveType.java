package com.homing_labs.cq.lang.java.model;

import hue.captains.singapura.tao.ontology.Stateless;

/**
 * One of the eight primitive types.
 */
public sealed interface PrimitiveType extends Stateless {

    record Boolean() implements PrimitiveType {
        public static final Boolean INSTANCE = new Boolean();
    }

    record Byte() implements PrimitiveType {
        public static final Byte INSTANCE = new Byte();
    }

    record Char() implements PrimitiveType {
        public static final Char INSTANCE = new Char();
    }

    record Short() implements PrimitiveType {
        public static final Short INSTANCE = new Short();
    }

    record Int() implements PrimitiveType {
        public static final Int INSTANCE = new Int();
    }

    record Long() implements PrimitiveType {
        public static final Long INSTANCE = new Long();
    }

    record Float() implements PrimitiveType {
        public static final Float INSTANCE = new Float();
    }

    record Double() implements PrimitiveType {
        public static final Double INSTANCE = new Double();
    }
}
