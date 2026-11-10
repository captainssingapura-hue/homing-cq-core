package com.homing_labs.cq.lang.java.outline;

import hue.captains.singapura.tao.ontology.StatelessFunctionalObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * A method's or a constructor's parameters, in their parentheses, read where a scan is (JLS 8.4.1): the receiver
 * first, if it is written, and the formal ones; and what the grammar asks of them.
 */
record ParameterLists() implements StatelessFunctionalObject {

    static final ParameterLists INSTANCE = new ParameterLists();

    /** The parameters, in their parentheses (JLS 8.4.1): the receiver first, if it is written, and the formal ones. */
    List<Parameter> read(final TokenScan scan) {
        final List<Parameter> parameters = new ArrayList<>();
        scan.advance();
        while (scan.at() < scan.size() && !scan.is(")")) {
            final Optional<Parameter> parameter = parameter(scan, parameters.isEmpty());
            if (parameter.isEmpty()) {
                closeParameters(scan);
                return parameters;
            }
            parameters.add(parameter.get());
            if (scan.is(",")) {
                scan.advance();
            } else if (!scan.is(")")) {
                scan.error("',', ')', or '[' expected", scan.at(), Math.min(scan.at() + 1, scan.size()));
                closeParameters(scan);
                return parameters;
            }
        }
        if (scan.is(")")) {
            scan.advance();
        } else {
            scan.error("')' expected", scan.at(), Math.min(scan.at() + 1, scan.size()));
        }
        checkVariableArity(scan, parameters);
        return parameters;
    }

    /** One parameter, the receiver or a formal one; none, when no type is written where its type would be. */
    private Optional<Parameter> parameter(final TokenScan scan, final boolean first) {
        final int start = scan.at();
        final List<ModifierItem> modifiers = scan.modifiers();
        TypeText parameterType = WrittenTypes.INSTANCE.type(scan);
        if (parameterType.range().isEmpty()) {
            return Optional.empty();
        }
        final boolean variable = scan.is(WrittenTypes.INSTANCE.pastAnnotations(scan, scan.at()), "...");
        if (variable) {
            while (scan.is("@")) {
                scan.annotation();
            }
            parameterType = new TypeText(new TokenRange(parameterType.range().from(), scan.last() + 1));
            scan.advance();
        }
        if (scan.is("this") || scan.identifier(scan.at()) && scan.is(scan.next(scan.at()), ".")
                && scan.is(scan.next(scan.next(scan.at())), "this")) {
            return Optional.of(receiver(scan, start, modifiers, parameterType, first));
        }
        final Name name = scan.simpleName();
        final TokenRange dimensions = WrittenTypes.INSTANCE.dimensions(scan);
        final Modifiers written = new Modifiers(modifiers);
        HeaderChecks.INSTANCE.checkModifiers(scan, written, Set.of("final"), false,
                new TokenRange(start, scan.last() + 1));
        return Optional.of(new Parameter.Formal(written, parameterType, variable, name, dimensions,
                new TokenRange(start, scan.last() + 1)));
    }

    /** The receiver parameter, from its name on: {@code this}, alone or after a class's name (JLS 8.4.1). */
    private Parameter.Receiver receiver(final TokenScan scan, final int start, final List<ModifierItem> modifiers,
                                        final TypeText parameterType, final boolean first) {
        scan.annotationsOnly(modifiers);
        if (!first) {
            scan.error("'this' names the receiver parameter only, which comes first", scan.at(), scan.at() + 1);
        }
        final int nameStart = scan.at();
        final StringBuilder text = new StringBuilder();
        while (!scan.is("this")) {
            text.append(scan.text(scan.at()));
            scan.advance();
        }
        scan.advance();
        return new Parameter.Receiver(new Modifiers(modifiers), parameterType,
                new Name(text.append("this").toString(), new TokenRange(nameStart, scan.last() + 1)),
                new TokenRange(start, scan.last() + 1));
    }

    /** Only the last formal parameter may be a variable arity one. */
    private void checkVariableArity(final TokenScan scan, final List<Parameter> parameters) {
        final List<Parameter.Formal> formal = new Parameters(parameters).formal();
        for (int index = 0; index + 1 < formal.size(); index++) {
            if (formal.get(index).variable()) {
                scan.error("varargs parameter must be the last parameter", formal.get(index).range());
            }
        }
    }

    /** Steps over the parenthesis that closes the parameters; or up to a brace or a semicolon, which none can hold. */
    private void closeParameters(final TokenScan scan) {
        int depth = 0;
        while (scan.at() < scan.size() && !scan.is("{") && !scan.is(";")) {
            if (scan.is("(")) {
                depth++;
            } else if (scan.is(")") && depth-- == 0) {
                scan.advance();
                return;
            }
            scan.advance();
        }
    }
}
