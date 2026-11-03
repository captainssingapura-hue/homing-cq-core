package com.homing_labs.cq.lang.java.extract;

import hue.captains.singapura.tao.ontology.Vector;

import java.net.URI;
import java.util.List;
import java.util.stream.Stream;

/**
 * The directories a code base's sources are in, in the order they are searched: a module may have several, such as
 * the sources written and those an annotation processor generated.
 */
public record SourceDirectories(List<SourceDirectory> items) implements Vector<SourceDirectory> {

    public SourceDirectories {
        items = List.copyOf(items);
    }

    /** The directories at these locations, in order: none, when there is none. */
    public SourceDirectories(final URI... locations) {
        this(Stream.of(locations).map(SourceDirectory::new).toList());
    }

    /** These directories, then the others, each once. */
    public SourceDirectories then(final SourceDirectories others) {
        return new SourceDirectories(Stream.concat(items.stream(), others.items().stream()).distinct().toList());
    }
}
