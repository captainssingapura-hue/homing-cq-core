package com.homing_labs.cq.lang.java.extract;

import com.homing_labs.cq.lang.java.model.ClassName;
import com.homing_labs.cq.lang.java.outline.BodyPart;
import com.homing_labs.cq.lang.java.outline.BodyParts;

import java.util.Optional;

/** A class's body as the outline read it: its parts, in the tokens they were read from. */
record ReadBody(Lexed lexed, BodyParts parts) {

    /** A member type of the class, as its body declares it. */
    Optional<BodyPart.TypeDeclaration> memberType(final ClassName inClass, final ClassName name) {
        final String simpleName = name.binaryName().substring(inClass.binaryName().length() + 1);
        return parts.of(BodyPart.TypeDeclaration.class).stream()
                .filter(type -> type.header().name().text().equals(simpleName))
                .findFirst();
    }
}
