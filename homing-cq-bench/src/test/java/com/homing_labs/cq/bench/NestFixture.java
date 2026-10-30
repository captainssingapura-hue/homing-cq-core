package com.homing_labs.cq.bench;

/**
 * A class with each kind of class nested in it - anonymous, local, inner and static - and one nested a level deeper,
 * for the nest index.
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

    class Inner {

        class Deeper {
        }
    }

    static class Nested {
    }
}
