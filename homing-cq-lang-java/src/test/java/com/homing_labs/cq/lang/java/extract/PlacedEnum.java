package com.homing_labs.cq.lang.java.extract;

/** An enum, whose constants javac makes in its static initialiser, and whose constructors take two more parameters. */
enum PlacedEnum {
    ONE,
    TWO("two") {
        @Override
        String label() {
            return "second";
        }
    };

    private final String label;

    PlacedEnum() {
        this("one");
    }

    PlacedEnum(final String label) {
        this.label = label;
    }

    String label() {
        return label;
    }
}
