package com.homing_labs.cq.rules.basic.enforce;

import com.homing_labs.cq.core.def.lang.JavaLang;
import com.homing_labs.cq.rules.basic.rule.NoPublicFields;
import com.homing_labs.cq.enforcer.def.Findings;
import com.homing_labs.cq.lang.java.model.Attributes;
import com.homing_labs.cq.lang.java.model.ClassName;
import com.homing_labs.cq.lang.java.model.FieldValue;
import com.homing_labs.cq.lang.java.model.Visibility;
import com.homing_labs.cq.lang.java.model.WithSource;
import com.homing_labs.cq.lang.java.taxonomy.Field;
import hue.captains.singapura.tao.ontology.StatelessFunctionalObject;

import java.util.List;
import java.util.stream.Collectors;

/**
 * What {@link NoPublicFields} finds on one class, from its attributes: its public instance fields, if a class of its
 * kind can have any. The rule's enforcers share this, one for the top-level class and one for each class nested in it.
 */
record PublicFields() implements StatelessFunctionalObject {

    static final PublicFields INSTANCE = new PublicFields();

    Findings<JavaLang, NoPublicFields> in(final ClassName in, final Attributes attributes) {
        final List<WithSource<FieldValue>> fields = attributes.has(Field.INSTANCE)
                ? attributes.all(Field.INSTANCE, FieldValue.class) : List.of();
        return new Findings<>(fields.stream()
                .map(WithSource::object)
                .filter(field -> field.visibility() instanceof Visibility.Public)
                .map(field -> new PublicField(in, field.name()))
                .collect(Collectors.toSet()));
    }
}
