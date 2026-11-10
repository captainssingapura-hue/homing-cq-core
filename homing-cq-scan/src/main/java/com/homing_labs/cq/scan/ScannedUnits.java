package com.homing_labs.cq.scan;

import hue.captains.singapura.tao.ontology.Vector;

import java.util.List;

/** The units a scan found, by class name. */
public record ScannedUnits(List<ScannedUnit> items) implements Vector<ScannedUnit> {

    public ScannedUnits {
        items = List.copyOf(items);
    }
}
