package com.homing_labs.cq.scan.declarations;

import com.homing_labs.cq.scan.Declaration;

import java.util.List;

/** The taxonomy's code, declared alone; found by its {@code INSTANCE}. */
public record TaxonomyCode() implements Declaration {

    public static final TaxonomyCode INSTANCE = new TaxonomyCode();

    @Override
    public List<String> packages() {
        return List.of("com.homing_labs.cq.lang.java.taxonomy");
    }
}
