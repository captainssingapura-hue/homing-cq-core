package com.homing_labs.cq.rules.basic.encapsulation;

/**
 * Exposes a field at every level: on the class, on a static nested class, on an inner class, and on a class nested in
 * that. Its constant is static, so not an instance field.
 */
public class Leaky {

    public static final int LIMIT = 10;

    public int count;

    private int hidden;

    public static class Settings {

        public String name;
    }

    public class Cursor {

        public int position;

        class Deeper {

            public int depth;
        }
    }
}
