package com.homing_labs.cq.scan;

import com.homing_labs.cq.core.def.lang.JavaLang;
import com.homing_labs.cq.enforcer.def.Enforcer;
import com.homing_labs.cq.enforcer.def.Enforcers;
import com.homing_labs.cq.enforcer.def.Findings;
import com.homing_labs.cq.enforcer.def.Notice;
import com.homing_labs.cq.enforcer.def.Notices;
import com.homing_labs.cq.enforcer.def.Result;
import com.homing_labs.cq.lang.java.taxonomy.ClassKind;
import com.homing_labs.cq.lang.java.taxonomy.Placement;
import com.homing_labs.cq.lang.java.unit.JavaUnit;
import com.homing_labs.cq.lang.java.unit.JavaUnits;
import com.homing_labs.cq.scan.rules.EveryUnitIsSeen;
import com.homing_labs.cq.scan.rules.EveryUnitIsSeenEnforcer;
import com.homing_labs.cq.scan.rules.InterfacesAreSeenEnforcer;
import com.homing_labs.cq.scan.rules.Seen;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RuleChecksTest {

    @Test
    void aUnitCompliesWhenEveryRuleAppliedToItRanAndFoundNothing() {
        final JavaUnit classKind = JavaUnits.INSTANCE.of(ClassKind.class, Optional.empty());
        final JavaUnit placement = JavaUnits.INSTANCE.of(Placement.class, Optional.empty());
        final List<Enforcer<?, ?, ?>> both = List.of(EveryUnitIsSeenEnforcer.INSTANCE, new InterfacesAreSeenEnforcer());
        assertEquals(new Compliance(0, 1, 2), RuleChecks.INSTANCE.compliance(both, classKind));
        assertEquals(new Compliance(0, 1, 1), RuleChecks.INSTANCE.compliance(both, placement));
        assertEquals(new Compliance(1, 1, 0), RuleChecks.INSTANCE.compliance(List.of(), classKind),
                "no rule applies");
        final Applied failed = new Applied(Enforcers.INSTANCE.signatureOf(EveryUnitIsSeenEnforcer.INSTANCE),
                Optional.empty(), Optional.of("it failed"));
        assertEquals(new Compliance(0, 1, 0), new Compliance(List.of(failed)), "a rule that failed cannot vouch");
        assertEquals(new Compliance(3, 7, 9), new Compliance(1, 2, 4).plus(new Compliance(2, 5, 5)));
    }

    @Test
    void aRuleAppliedHasItsFindingsApartFromItsNoticesEachWithoutPackages() {
        final Applied applied = new Applied(Enforcers.INSTANCE.signatureOf(EveryUnitIsSeenEnforcer.INSTANCE),
                Optional.of(new Result<JavaLang, EveryUnitIsSeen>(new Findings<>(Set.of(new Seen<>("ClassKind"))),
                        new Notices(Set.of(new Notice.SourceUnavailable(ClassKind.class.getName()))))),
                Optional.empty());
        assertEquals(List.of("Seen[unit=ClassKind]"), applied.findings());
        assertEquals(List.of("Seen[unit=ClassKind]", "notice: SourceUnavailable[of=ClassKind]"), applied.lines());
        final Applied failed = new Applied(Enforcers.INSTANCE.signatureOf(EveryUnitIsSeenEnforcer.INSTANCE),
                Optional.empty(), Optional.of("it failed"));
        assertEquals(List.of(), failed.findings(), "a rule that failed found nothing");
        assertEquals(List.of("it failed"), failed.lines());
    }
}
