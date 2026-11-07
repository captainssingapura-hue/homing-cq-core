package com.homing_labs.cq.demo.exempted;

import com.homing_labs.cq.rules.basic.rule.NoPublicFields;
import com.homing_labs.cq.scan.Exemption;
import com.homing_labs.cq.scan.Exemptions;

import java.util.Set;

/** What {@link WireMessage} is let off: {@link NoPublicFields}, as its field is {@link FilledInByASerialiser}. */
public record WireMessageExemptions() implements Exemptions<WireMessage> {

    public static final WireMessageExemptions INSTANCE = new WireMessageExemptions();

    @Override
    public Set<Exemption<WireMessage, ?, ?>> exemptions() {
        return Set.of(new Exemption<>(WireMessage.class, NoPublicFields.class, FilledInByASerialiser.class));
    }
}
