package com.homing_labs.cq.lang.java.taxonomy.scan;

import java.time.DayOfWeek;

/**
 * A class with a classic switch on another class's enum, for which javac writes a class of its own, flagged synthetic,
 * to hold the switch map; and beside it an anonymous class the source declares.
 */
class SwitchFixture {

    final Runnable anonymous = new Runnable() {
        @Override
        public void run() {
        }
    };

    int weekend(final DayOfWeek day) {
        switch (day) {
            case SATURDAY:
            case SUNDAY:
                return 1;
            default:
                return 0;
        }
    }
}
