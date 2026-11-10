package com.homing_labs.cq.lang.java.outline;

import com.homing_labs.cq.lang.java.lexer.TokenStream;
import hue.captains.singapura.tao.ontology.StatelessFunctionalObject;

/**
 * Reads an ordinary compilation unit (JLS 7.3) from its tokens: its package declaration, its imports and its types,
 * each type's header in full, and its body down to its parts - each member's header in full, and what follows it, a
 * method's body say, left as tokens for a later stage. A member type's body is read so too, one level down, from a
 * block of code copied out of the file by the {@link NestedClassParser}; a type nested deeper is read to its header.
 *
 * <p>Nothing stops it. What breaks the grammar is a diagnostic, and the reading goes on from the next token a part can
 * start with. A module declaration, or a method or a field outside any class - the other two forms a compilation
 * unit can take - is said not to be read yet, and the rest of the file is left unread.</p>
 */
public record JavaOutliner() implements StatelessFunctionalObject {

    public static final JavaOutliner INSTANCE = new JavaOutliner();

    /** The file's parts, and what was found wrong in them. */
    public Outline outline(final TokenStream tokens) {
        return new FileScan(tokens).run();
    }
}
