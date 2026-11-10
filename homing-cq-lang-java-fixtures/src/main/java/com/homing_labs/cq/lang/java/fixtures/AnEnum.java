package com.homing_labs.cq.lang.java.fixtures;

/** A top-level enum; a constant with a body is a nested class of it. */
public enum AnEnum {
    PLAIN,
    WITH_BODY {
        @Override
        public String toString() {
            return "with body";
        }
    }
}
