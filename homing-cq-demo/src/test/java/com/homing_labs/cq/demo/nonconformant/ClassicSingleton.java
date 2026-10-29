package com.homing_labs.cq.demo.nonconformant;

/**
 * The lazily made singleton: a static field set on first use, behind a static accessor.
 */
public class ClassicSingleton {

    private static ClassicSingleton instance;

    private ClassicSingleton() {
    }

    public static synchronized ClassicSingleton getInstance() {
        if (instance == null) {
            instance = new ClassicSingleton();
        }
        return instance;
    }
}
