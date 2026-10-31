package com.homing_labs.cq.bench;

/**
 * A class with each kind of class nested in it - anonymous, local, inner and static - and two nested a level deeper,
 * one a member and one local, for the nest index and the class object.
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
