package com.homing_labs.cq.scan;

import hue.captains.singapura.tao.ontology.Vector;

import java.util.List;

/** The report table's rows: by class, then rule, then findings, notices and what was thrown, each by its text. */
public record ReportRows(List<ReportRow> items) implements Vector<ReportRow> {

    public ReportRows {
        items = List.copyOf(items);
    }
}
