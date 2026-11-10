package com.homing_labs.cq.bench;

import java.time.DayOfWeek;

/**
 * A class with each kind of class nested in it - anonymous, local, inner and static - and two nested a level deeper,
 * one a member and one local, for the nest index and the class object. Its switch makes javac write one more, which
 * the source does not declare.
 */
class NestFixture {

    final Runnable anonymous = new Runnable() {
        @Override
        public void run() {
        }
    };

    void declaresALocalClass() {
        class Local {
        }
    }

    /** A classic switch on another class's enum: javac holds its switch map in a synthetic class of its own. */
    int weekend(final DayOfWeek day) {
        switch (day) {
            case SATURDAY:
            case SUNDAY:
                return 1;
            default:
                return 0;
        }
    }

    class Inner {

        Object make() {
            class Made {
            }
            return new Made();
        }

        class Deeper {
        }
    }

    static class Nested {
    }
}
