package com.homing_labs.cq.scan.declarations;

import com.homing_labs.cq.scan.Declaration;

import java.util.List;

/** Java's plain definitions, which declare no interface, declared alone. */
public record DefinitionCode() implements Declaration {

    public static final DefinitionCode INSTANCE = new DefinitionCode();

    @Override
    public List<String> packages() {
        return List.of("com.homing_labs.cq.lang.java.def");
    }
}
