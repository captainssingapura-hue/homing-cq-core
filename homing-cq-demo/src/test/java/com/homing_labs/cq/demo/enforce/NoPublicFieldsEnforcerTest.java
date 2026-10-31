package com.homing_labs.cq.demo.enforce;

import com.homing_labs.cq.core.def.lang.JavaLang;
import com.homing_labs.cq.demo.encapsulation.Leaky;
import com.homing_labs.cq.demo.encapsulation.Tight;
import com.homing_labs.cq.demo.rule.NoPublicFields;
import com.homing_labs.cq.enforcer.def.Findings;
import com.homing_labs.cq.enforcer.def.Result;
import com.homing_labs.cq.lang.java.unit.ClassUnit;
import com.homing_labs.cq.lang.java.unit.JavaNestedUnit;
import com.homing_labs.cq.lang.java.unit.JavaNests;
import com.homing_labs.cq.lang.java.unit.JavaUnits;
import com.homing_labs.cq.lang.java.model.ClassName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

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

    private ClassUnit unit(final Class<?> type) {
        return (ClassUnit) JavaUnits.INSTANCE.of(type, Optional.empty());
    }

    @SafeVarargs
    private Result<JavaLang, NoPublicFields> found(final PublicField... fields) {
        return new Result<>(new Findings<>(Set.of(fields)));
    }
}
