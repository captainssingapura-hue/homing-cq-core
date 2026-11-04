package com.homing_labs.cq.demo;

import com.homing_labs.cq.rules.basic.BasicRules;
import com.homing_labs.cq.scan.Declaration;
import com.homing_labs.cq.scan.ReactorSources;
import hue.captains.singapura.tao.ontology.StatelessFunctionalObject;

import java.nio.file.Path;
import java.util.List;

/**
 * The code the demo checks: all the main code under {@code com.homing_labs.cq} - the framework, the bench's own
 * included, and this demo - with the sources of every module of the reactor it is built in, so they are found even
 * where the framework's classes come from jars in a repository. The demo opts in to the basic rules as any project
 * does: composed with {@link BasicRules}, this is what the bench and the self-check take, and {@code mvn javafx:run} in
 * this module names the two.
 */
public record DemoCode() implements Declaration, StatelessFunctionalObject {

    public static final DemoCode INSTANCE = new DemoCode();

    @Override
    public List<String> packages() {
        return List.of("com.homing_labs.cq");
    }

    @Override
    public List<Path> sources() {
        return new ReactorSources(DemoCode.class).sources();
    }
}
