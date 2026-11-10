package com.homing_labs.cq.lang.java.extract;

import java.util.List;

/** A type parameter: its name, and its bounds by their names - as written, or as a signature declares them. */
record Variable(String name, List<String> bounds) {
}
