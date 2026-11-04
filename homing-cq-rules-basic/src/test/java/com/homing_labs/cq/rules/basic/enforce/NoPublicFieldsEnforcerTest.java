package com.homing_labs.cq.rules.basic.enforce;

import com.homing_labs.cq.core.def.lang.JavaLang;
import com.homing_labs.cq.rules.basic.encapsulation.Leaky;
import com.homing_labs.cq.rules.basic.encapsulation.LeakyAbstract;
import com.homing_labs.cq.rules.basic.encapsulation.LeakyAnnotation;
import com.homing_labs.cq.rules.basic.encapsulation.LeakyEnum;
import com.homing_labs.cq.rules.basic.encapsulation.LeakyInterface;
import com.homing_labs.cq.rules.basic.encapsulation.LeakyRecord;
import com.homing_labs.cq.rules.basic.encapsulation.Tight;
import com.homing_labs.cq.rules.basic.rule.NoPublicFields;
import com.homing_labs.cq.enforcer.def.Enforcers;
import com.homing_labs.cq.enforcer.def.Findings;
import com.homing_labs.cq.enforcer.def.Result;
import com.homing_labs.cq.lang.java.unit.AbstractClassUnit;
import com.homing_labs.cq.lang.java.unit.AnnotationUnit;
import com.homing_labs.cq.lang.java.unit.ClassUnit;
import com.homing_labs.cq.lang.java.unit.EnumUnit;
import com.homing_labs.cq.lang.java.unit.InterfaceUnit;
import com.homing_labs.cq.lang.java.unit.JavaNestedUnit;
import com.homing_labs.cq.lang.java.unit.JavaNests;
import com.homing_labs.cq.lang.java.unit.JavaUnit;
import com.homing_labs.cq.lang.java.unit.JavaUnits;
import com.homing_labs.cq.lang.java.unit.RecordUnit;
import com.homing_labs.cq.lang.java.model.ClassName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NoPublicFieldsEnforcerTest {

    private static final String LEAKY = Leaky.class.getName();

    @Test
    void findsAPublicFieldAtEveryLevelOfTheNest() {
        assertEquals(found(new PublicField(new ClassName(LEAKY), "count"),
                        new PublicField(new ClassName(LEAKY + "$Settings"), "name"),
                        new PublicField(new ClassName(LEAKY + "$Cursor"), "position"),
                        new PublicField(new ClassName(LEAKY + "$Cursor$Deeper"), "depth")),
                NoPublicFieldsEnforcer.INSTANCE.check(unit(Leaky.class)));
    }

    @Test
    void findsNothingWhereEveryFieldIsKept() {
        assertEquals(found(), NoPublicFieldsEnforcer.INSTANCE.check(unit(Tight.class)));
    }

    @Test
    void theSubRuleChecksOneNestedClassAlone() {
        final List<JavaNestedUnit> nested = JavaNests.INSTANCE.of(unit(Leaky.class));
        assertEquals(List.of(LEAKY + "$Settings", LEAKY + "$Cursor", LEAKY + "$Cursor$Deeper"),
                nested.stream().map(each -> each.nested().object().name().binaryName()).toList());
        assertEquals(found(new PublicField(new ClassName(LEAKY + "$Cursor"), "position")),
                NoPublicFieldsInNestedClassEnforcer.INSTANCE.check(nested.get(1)));
    }

    @Test
    void anAbstractClassIsCheckedLikeAConcreteOneAndAnEnumNestedInIt() {
        final String leaky = LeakyAbstract.class.getName();
        assertEquals(found(new PublicField(new ClassName(leaky), "count"),
                        new PublicField(new ClassName(leaky + "$Mode"), "level")),
                NoPublicFieldsEnforcer.INSTANCE.check(unit(LeakyAbstract.class)));
    }

    @Test
    void anEnumIsCheckedAndAnAbstractClassNestedInItButNotItsConstants() {
        final String leaky = LeakyEnum.class.getName();
        assertEquals(found(new PublicField(new ClassName(leaky), "count"),
                        new PublicField(new ClassName(leaky + "$Base"), "base")),
                NoPublicFieldsEnforcer.INSTANCE.check(unit(LeakyEnum.class)));
    }

    @Test
    void aClassNestedInARecordIsCheckedButNotTheRecordsComponents() {
        assertEquals(found(new PublicField(new ClassName(LeakyRecord.class.getName() + "$Part"), "size")),
                NoPublicFieldsEnforcer.INSTANCE.check(unit(LeakyRecord.class)));
    }

    @Test
    void aClassNestedInAnInterfaceIsCheckedButNotItsConstants() {
        assertEquals(found(new PublicField(new ClassName(LeakyInterface.class.getName() + "$Part"), "size")),
                NoPublicFieldsEnforcer.INSTANCE.check(unit(LeakyInterface.class)));
    }

    @Test
    void aClassNestedInAnAnnotationIsChecked() {
        assertEquals(found(new PublicField(new ClassName(LeakyAnnotation.class.getName() + "$Part"), "size")),
                NoPublicFieldsEnforcer.INSTANCE.check(unit(LeakyAnnotation.class)));
    }

    @Test
    void theEnforcerTakesAUnitOfEveryKind() {
        final Map<Class<?>, Class<? extends JavaUnit>> kinds = Map.of(
                Leaky.class, ClassUnit.class,
                LeakyAbstract.class, AbstractClassUnit.class,
                LeakyEnum.class, EnumUnit.class,
                LeakyRecord.class, RecordUnit.class,
                LeakyInterface.class, InterfaceUnit.class,
                LeakyAnnotation.class, AnnotationUnit.class);
        kinds.forEach((type, kind) -> {
            final JavaUnit unit = unit(type);
            assertEquals(kind, unit.getClass());
            assertTrue(Enforcers.INSTANCE.signatureOf(NoPublicFieldsEnforcer.INSTANCE).checks(unit), type.getName());
        });
    }

    private JavaUnit unit(final Class<?> type) {
        return JavaUnits.INSTANCE.of(type, Optional.empty());
    }

    @SafeVarargs
    private Result<JavaLang, NoPublicFields> found(final PublicField... fields) {
        return new Result<>(new Findings<>(Set.of(fields)));
    }
}
