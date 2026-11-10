package com.homing_labs.cq.lang.java.fixtures;

import java.util.function.Supplier;

/** A top-level class with every kind of nested class inside it: one unit, five classes. */
public class Nesting {

    /** A member class. */
    public class Member {
    }

    /** A static nested class. */
    public static class StaticNested {
    }

    /** Declares a local class and an anonymous class. */
    public Object localAndAnonymous() {
        class Local {
        }
        final Runnable anonymous = new Runnable() {
            @Override
            public void run() {
            }
        };
        final Supplier<Object> lambdaIsNoClass = Local::new;
        return java.util.List.of(lambdaIsNoClass.get(), anonymous);
    }
}
