package com.homing_labs.cq.lang.java;

import com.homing_labs.cq.lang.java.unit.JavaUnit;
import com.homing_labs.cq.lang.java.extract.JavaUnitReader;
import hue.captains.singapura.tao.ontology.FunctionalObject;
import hue.captains.singapura.tao.ontology.Immutable;
import hue.captains.singapura.tao.ontology.Mutable;
import hue.captains.singapura.tao.ontology.Stateless;
import hue.captains.singapura.tao.ontology.ValueObject;
import hue.captains.singapura.tao.ontology.enforcer.ContractViolation;
import hue.captains.singapura.tao.ontology.enforcer.OntologyEnforcer;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * This module read by its own reader, and every public type in it held to what it says it is.
 */
class OwnModuleTest {

    private final List<JavaUnit> units = JavaUnitReader.INSTANCE.read(JavaUnit.class);

    @Test
    void readsItselfWithItsSources() {
        assertEquals(List.of(
                        "com.homing_labs.cq.lang.java.def.JavaField",
                        "com.homing_labs.cq.lang.java.def.JavaMethod",
                        "com.homing_labs.cq.lang.java.def.JavaSource",
                        "com.homing_labs.cq.lang.java.def.JavaType",
                        "com.homing_labs.cq.lang.java.def.JavaTypes",
                        "com.homing_labs.cq.lang.java.extract.Candidate",
                        "com.homing_labs.cq.lang.java.extract.ClassFileAnnotations",
                        "com.homing_labs.cq.lang.java.extract.ClassFileAttributes",
                        "com.homing_labs.cq.lang.java.extract.ClassFileTypes",
                        "com.homing_labs.cq.lang.java.extract.HeaderSources",
                        "com.homing_labs.cq.lang.java.extract.Initialisers",
                        "com.homing_labs.cq.lang.java.extract.JavaCodeBase",
                        "com.homing_labs.cq.lang.java.extract.JavaCodeBases",
                        "com.homing_labs.cq.lang.java.extract.JavaUnitReader",
                        "com.homing_labs.cq.lang.java.extract.Lexed",
                        "com.homing_labs.cq.lang.java.extract.MemberSources",
                        "com.homing_labs.cq.lang.java.extract.Overloads",
                        "com.homing_labs.cq.lang.java.extract.ReadBody",
                        "com.homing_labs.cq.lang.java.extract.SourceLocator",
                        "com.homing_labs.cq.lang.java.extract.SourceReading",
                        "com.homing_labs.cq.lang.java.extract.TypeNames",
                        "com.homing_labs.cq.lang.java.extract.Variable",
                        "com.homing_labs.cq.lang.java.model.Annotation",
                        "com.homing_labs.cq.lang.java.model.Annotations",
                        "com.homing_labs.cq.lang.java.model.AppliedAnnotation",
                        "com.homing_labs.cq.lang.java.model.Arity",
                        "com.homing_labs.cq.lang.java.model.Attributes",
                        "com.homing_labs.cq.lang.java.model.ClassInitialisation",
                        "com.homing_labs.cq.lang.java.model.ClassName",
                        "com.homing_labs.cq.lang.java.model.ClassNames",
                        "com.homing_labs.cq.lang.java.model.CodeBlocks",
                        "com.homing_labs.cq.lang.java.model.ConstructorSignature",
                        "com.homing_labs.cq.lang.java.model.ConstructorValue",
                        "com.homing_labs.cq.lang.java.model.Element",
                        "com.homing_labs.cq.lang.java.model.ElementValue",
                        "com.homing_labs.cq.lang.java.model.ElementValues",
                        "com.homing_labs.cq.lang.java.model.Elements",
                        "com.homing_labs.cq.lang.java.model.EnclosedClass",
                        "com.homing_labs.cq.lang.java.model.Enclosure",
                        "com.homing_labs.cq.lang.java.model.Entries",
                        "com.homing_labs.cq.lang.java.model.Entry",
                        "com.homing_labs.cq.lang.java.model.EnumConstantValue",
                        "com.homing_labs.cq.lang.java.model.Extensibility",
                        "com.homing_labs.cq.lang.java.model.FieldValue",
                        "com.homing_labs.cq.lang.java.model.InitialisationStep",
                        "com.homing_labs.cq.lang.java.model.InitialisationSteps",
                        "com.homing_labs.cq.lang.java.model.MethodSignature",
                        "com.homing_labs.cq.lang.java.model.MethodValue",
                        "com.homing_labs.cq.lang.java.model.Mutability",
                        "com.homing_labs.cq.lang.java.model.NestedClass",
                        "com.homing_labs.cq.lang.java.model.Origin",
                        "com.homing_labs.cq.lang.java.model.Overridability",
                        "com.homing_labs.cq.lang.java.model.ParameterAnnotation",
                        "com.homing_labs.cq.lang.java.model.ParameterAnnotations",
                        "com.homing_labs.cq.lang.java.model.Parameters",
                        "com.homing_labs.cq.lang.java.model.Persistence",
                        "com.homing_labs.cq.lang.java.model.PrimitiveType",
                        "com.homing_labs.cq.lang.java.model.ProcessedSource",
                        "com.homing_labs.cq.lang.java.model.RecordComponentValue",
                        "com.homing_labs.cq.lang.java.model.Retention",
                        "com.homing_labs.cq.lang.java.model.Synchronisation",
                        "com.homing_labs.cq.lang.java.model.TypeArgument",
                        "com.homing_labs.cq.lang.java.model.TypeArguments",
                        "com.homing_labs.cq.lang.java.model.TypeParameterValue",
                        "com.homing_labs.cq.lang.java.model.TypeParameters",
                        "com.homing_labs.cq.lang.java.model.TypeUse",
                        "com.homing_labs.cq.lang.java.model.TypeUses",
                        "com.homing_labs.cq.lang.java.model.ValueTypes",
                        "com.homing_labs.cq.lang.java.model.Values",
                        "com.homing_labs.cq.lang.java.model.Visibility",
                        "com.homing_labs.cq.lang.java.model.WithSource",
                        "com.homing_labs.cq.lang.java.model.WrittenParameter",
                        "com.homing_labs.cq.lang.java.model.WrittenParameters",
                        "com.homing_labs.cq.lang.java.unit.AbstractClassUnit",
                        "com.homing_labs.cq.lang.java.unit.AnnotationUnit",
                        "com.homing_labs.cq.lang.java.unit.ClassUnit",
                        "com.homing_labs.cq.lang.java.unit.EnumUnit",
                        "com.homing_labs.cq.lang.java.unit.Identity",
                        "com.homing_labs.cq.lang.java.unit.InterfaceUnit",
                        "com.homing_labs.cq.lang.java.unit.JavaNestedUnit",
                        "com.homing_labs.cq.lang.java.unit.JavaNests",
                        "com.homing_labs.cq.lang.java.unit.JavaUnit",
                        "com.homing_labs.cq.lang.java.unit.JavaUnits",
                        "com.homing_labs.cq.lang.java.unit.RecordUnit"),
                units.stream().map(unit -> unit.type().getName()).toList());
        assertTrue(units.stream().allMatch(unit -> unit.source().isPresent()));
    }

    @Test
    void everyPublicTypeSaysWhatItIsAndIsHeldToIt() {
        final List<String> violations = new ArrayList<>();
        for (final JavaUnit unit : units) {
            for (final Class<?> type : unit.type().getNestMembers()) {
                if (!Modifier.isPublic(type.getModifiers())) {
                    continue;
                }
                if (type.isInterface()) {
                    if (!saysWhatItIs(type)) {
                        violations.add(type.getName() + " extends no jOntology marker");
                    }
                } else {
                    new OntologyEnforcer().enforce(type).stream().map(ContractViolation::message).forEach(violations::add);
                }
            }
        }
        assertEquals(List.of(), violations);
    }

    private boolean saysWhatItIs(final Class<?> type) {
        return Stream.of(Stateless.class, FunctionalObject.class, ValueObject.class, Immutable.class, Mutable.class)
                .anyMatch(marker -> marker.isAssignableFrom(type));
    }
}
