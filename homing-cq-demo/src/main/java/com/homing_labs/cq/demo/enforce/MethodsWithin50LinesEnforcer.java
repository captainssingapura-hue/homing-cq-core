package com.homing_labs.cq.demo.enforce;

import com.homing_labs.cq.core.def.lang.JavaLang;
import com.homing_labs.cq.demo.rule.MethodsWithin50Lines;
import com.homing_labs.cq.enforcer.def.Enforcer;
import com.homing_labs.cq.enforcer.def.Findings;
import com.homing_labs.cq.enforcer.def.Notice;
import com.homing_labs.cq.enforcer.def.Result;
import com.homing_labs.cq.lang.java.model.Attributes;
import com.homing_labs.cq.lang.java.model.ClassName;
import com.homing_labs.cq.lang.java.model.EnclosedClass;
import com.homing_labs.cq.lang.java.model.MethodValue;
import com.homing_labs.cq.lang.java.model.NestedClass;
import com.homing_labs.cq.lang.java.model.ProcessedSource;
import com.homing_labs.cq.lang.java.model.WrittenParameters;
import com.homing_labs.cq.lang.java.outline.CodeBlock;
import com.homing_labs.cq.lang.java.unit.JavaUnit;
import hue.captains.singapura.tao.ontology.StatelessFunctionalObject;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Checks {@link MethodsWithin50Lines} on a Java unit of any kind, from the sources its values were given: each method
 * and constructor written in its top-level class or in a member class of it, by the effective lines of the block it is
 * written in. One in a class nested deeper, or in a local or anonymous class, is written within another member, and is
 * counted there. A unit without its source is not checked, and says so.
 */
public record MethodsWithin50LinesEnforcer()
        implements Enforcer<JavaLang, MethodsWithin50Lines, JavaUnit>, StatelessFunctionalObject {

    public static final MethodsWithin50LinesEnforcer INSTANCE = new MethodsWithin50LinesEnforcer();

    @Override
    public Result<JavaLang, MethodsWithin50Lines> check(final JavaUnit unit) {
        if (unit.source().isEmpty()) {
            return new Result<JavaLang, MethodsWithin50Lines>(new Findings<>(Set.of()))
                    .noting(new Notice.SourceUnavailable(unit.identity().name().binaryName()));
        }
        final LineCounts lines = EffectiveLines.INSTANCE.of(unit.source().get().text());
        return new Result<>(new Findings<>(found(unit.identity().name(), unit.attributes(), lines)
                .collect(Collectors.toSet())));
    }

    /** The methods and constructors of a class, and of the classes nested in it, longer than the rule allows. */
    private Stream<LongMethod> found(final ClassName in, final Attributes attributes, final LineCounts lines) {
        return attributes.entries().stream()
                .flatMap(entry -> entry.all().stream())
                .flatMap(value -> switch (value.source()) {
                    case ProcessedSource.OfMethod method -> longer(in, ((MethodValue) value.object()).name(),
                            method.block(), method.parameters(), lines);
                    case ProcessedSource.OfConstructor constructor -> longer(in,
                            in.simpleName().substring(in.simpleName().lastIndexOf('$') + 1), constructor.block(),
                            constructor.parameters(), lines);
                    default -> switch (value.object()) {
                        case NestedClass nested -> found(nested.name(), nested.attributes(), lines);
                        case EnclosedClass enclosed -> found(enclosed.name(), enclosed.attributes(), lines);
                        default -> Stream.empty();
                    };
                });
    }

    /** A member, when it is written in more effective lines than the rule allows. */
    private Stream<LongMethod> longer(final ClassName in, final String name, final CodeBlock block,
                                      final WrittenParameters parameters, final LineCounts lines) {
        final int counted = lines.of(block);
        return counted > MethodsWithin50Lines.INSTANCE.limit()
                ? Stream.of(new LongMethod(in, name + parameters.items().stream()
                .map(parameter -> parameter.block().text().replaceAll("\\s+", " "))
                .collect(Collectors.joining(", ", "(", ")")), block.line(), counted))
                : Stream.empty();
    }
}
