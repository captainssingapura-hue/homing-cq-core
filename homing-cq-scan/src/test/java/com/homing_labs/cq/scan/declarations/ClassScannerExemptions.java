package com.homing_labs.cq.scan.declarations;

import com.homing_labs.cq.lang.java.taxonomy.scan.ClassScanner;
import com.homing_labs.cq.scan.Exemption;
import com.homing_labs.cq.scan.Exemptions;
import com.homing_labs.cq.scan.rules.EveryUnitIsSeen;
import com.homing_labs.cq.scan.rules.SeenOnPurpose;

import java.util.Set;

/** What the taxonomy's class scanner is let off, as a class's exemptions are declared. */
public record ClassScannerExemptions() implements Exemptions<ClassScanner> {

    public static final ClassScannerExemptions INSTANCE = new ClassScannerExemptions();

    @Override
    public Set<Exemption<ClassScanner, ?, ?>> exemptions() {
        return Set.of(new Exemption<>(ClassScanner.class, EveryUnitIsSeen.class, SeenOnPurpose.class));
    }
}
