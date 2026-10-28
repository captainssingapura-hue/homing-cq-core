package com.homing_labs.cq.enforcer.def;

import com.homing_labs.cq.core.def.Language;
import com.homing_labs.cq.core.def.QcUnit;
import com.homing_labs.cq.core.def.Rule;
import hue.captains.singapura.tao.ontology.FunctionalObject;

public interface Enforcer<L extends Language, R extends Rule<?,L>, U extends QcUnit<L>> extends FunctionalObject {
    Result<L,R> check(U qcUnit);
}
