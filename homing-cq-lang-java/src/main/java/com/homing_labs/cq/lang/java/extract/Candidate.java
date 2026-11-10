package com.homing_labs.cq.lang.java.extract;

import com.homing_labs.cq.lang.java.outline.BodyPart;
import com.homing_labs.cq.lang.java.outline.TokenRange;

import java.util.List;
import java.util.Optional;

/**
 * A method or a constructor as written: where, its type parameters, its parameters' types erased to the names
 * written, and its body.
 */
record Candidate(TokenRange range, List<Variable> typeParameters, List<String> parameters, Optional<TokenRange> body,
                 BodyPart part) {
}
