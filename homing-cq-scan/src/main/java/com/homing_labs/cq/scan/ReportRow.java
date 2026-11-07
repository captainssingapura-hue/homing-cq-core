package com.homing_labs.cq.scan;

import hue.captains.singapura.tao.ontology.Scalar;

import java.util.List;
import java.util.Optional;

/**
 * One row of the report's table: a finding, exempted or not, a notice, or a rule that threw, on one unit; or a stale
 * exemption of a class, scanned or not.
 *
 * @param id        the rule, the unit's class and a number counting up within the two, as {@code Rule:pack.Class#2},
 *                  which exempting a class does not change; or, for a stale exemption, {@code Rule:pack.Class#stale}
 * @param entry     {@code finding}, {@code exempted}, {@code notice}, {@code failure} or {@code stale}
 * @param unit      the unit's top-level class, by binary name
 * @param codebase  where the class was loaded from: a jar's file name, or the name of a module's folder
 * @param source    the unit's source file, with forward slashes, when it was read with one: none for a stale exemption
 * @param principle the principle's simple class name: empty for a stale exemption
 * @param rule      the rule's simple class name, as the gate prints it
 * @param finding   the finding's or the notice's simple class name, or how an exemption is stale: none for a rule that
 *                  threw
 * @param details   the text the gate prints for a finding, a notice's text, what the rule threw, or how an exemption is
 *                  stale
 * @param excuse    the excuse's simple class name, for an exempted finding or a stale exemption
 */
public record ReportRow(String id, String entry, String packageName, String unit, Optional<String> codebase,
                        Optional<String> source, String principle, String rule, Optional<String> finding,
                        String details, Optional<String> excuse) implements Scalar {

    /** The row's values in the table's order, each one absent as an empty cell. */
    public List<String> cells() {
        return List.of(id, entry, packageName, unit, codebase.orElse(""), source.orElse(""), principle, rule,
                finding.orElse(""), details, excuse.orElse(""));
    }
}
