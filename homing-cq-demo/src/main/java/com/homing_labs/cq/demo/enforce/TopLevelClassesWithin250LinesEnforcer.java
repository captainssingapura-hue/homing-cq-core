package com.homing_labs.cq.demo.enforce;

import com.homing_labs.cq.core.def.lang.JavaLang;
import com.homing_labs.cq.demo.rule.TopLevelClassesWithin250Lines;
import com.homing_labs.cq.enforcer.def.Enforcer;
import com.homing_labs.cq.enforcer.def.Findings;
import com.homing_labs.cq.enforcer.def.Notice;
import com.homing_labs.cq.enforcer.def.Result;
import com.homing_labs.cq.lang.java.def.JavaSource;
import com.homing_labs.cq.lang.java.extract.SourceLocator;
import com.homing_labs.cq.lang.java.outline.CodeBlock;
import com.homing_labs.cq.lang.java.unit.JavaUnit;
import hue.captains.singapura.tao.ontology.StatelessFunctionalObject;

import java.util.Optional;
import java.util.Set;

/**
 * Checks {@link TopLevelClassesWithin250Lines} on a Java unit of any kind, from its source: the effective lines its
 * top-level class is declared in, from its documentation comment to its closing brace. A unit without its source, or
 * whose source does not declare it, is not checked, and says so.
 */
public record TopLevelClassesWithin250LinesEnforcer()
        implements Enforcer<JavaLang, TopLevelClassesWithin250Lines, JavaUnit>, StatelessFunctionalObject {

    public static final TopLevelClassesWithin250LinesEnforcer INSTANCE = new TopLevelClassesWithin250LinesEnforcer();

    @Override
    public Result<JavaLang, TopLevelClassesWithin250Lines> check(final JavaUnit unit) {
        final Optional<JavaSource> source = unit.source();
        final Optional<CodeBlock> declaration = SourceLocator.INSTANCE.declaration(unit);
        if (source.isEmpty() || declaration.isEmpty()) {
            return new Result<JavaLang, TopLevelClassesWithin250Lines>(new Findings<>(Set.of()))
                    .noting(new Notice.SourceUnavailable(unit.identity().name().binaryName()));
        }
        final int lines = EffectiveLines.INSTANCE.of(source.get().text()).of(declaration.get());
        return new Result<>(new Findings<>(lines > TopLevelClassesWithin250Lines.INSTANCE.limit()
                ? Set.of(new LongClass(unit.identity().name(), lines)) : Set.of()));
    }
}
