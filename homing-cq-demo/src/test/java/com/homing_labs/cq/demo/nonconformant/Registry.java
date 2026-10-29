package com.homing_labs.cq.demo.nonconformant;

import java.util.HashMap;
import java.util.Map;

/**
 * A process-wide registry: a final map whose contents anyone can change, filled by a static method.
 */
public class Registry {

    private static final Map<String, Object> ENTRIES = new HashMap<>();

    public static void register(final String key, final Object value) {
        ENTRIES.put(key, value);
    }

    public Object lookup(final String key) {
        return ENTRIES.get(key);
    }
}
