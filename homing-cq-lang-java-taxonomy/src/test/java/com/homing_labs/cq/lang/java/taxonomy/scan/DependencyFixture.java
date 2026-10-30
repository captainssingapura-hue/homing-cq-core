package com.homing_labs.cq.lang.java.taxonomy.scan;

import java.net.HttpURLConnection;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * A class to find dependencies in, with each kind it can have: an inlined constant from {@link HttpURLConnection}, an
 * annotation, a field of type {@link Duration}, a local whose type argument {@link Instant} is named nowhere but in the
 * debug tables, code making an {@link ArrayList}, and a permitted subclass, nested in it.
 */
@Deprecated
sealed class DependencyFixture permits DependencyFixture.Leaf {

    static final int OK = HttpURLConnection.HTTP_OK;

    Duration timeout;

    int count() {
        final List<Instant> instants = new ArrayList<>();
        return instants.size();
    }

    static final class Leaf extends DependencyFixture {
    }
}
