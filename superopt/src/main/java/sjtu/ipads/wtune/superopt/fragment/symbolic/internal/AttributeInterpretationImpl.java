package sjtu.ipads.wtune.superopt.fragment.symbolic.internal;

import sjtu.ipads.wtune.sqlparser.plan.AttributeDef;
import sjtu.ipads.wtune.superopt.fragment.symbolic.AttributeInterpretation;

import java.util.List;

public class AttributeInterpretationImpl extends InterpretationBase<List<AttributeDef>>
    implements AttributeInterpretation {
  protected AttributeInterpretationImpl(List<AttributeDef> object) {
    super(object);
  }

  @Override
  public boolean isCompatible(List<AttributeDef> thatDefs) {
    return object().equals(thatDefs);
  }

  @Override
  public boolean shouldOverride(List<AttributeDef> thatDefs) {
    final List<AttributeDef> thisDefs = object();
    assert thisDefs.size() == thatDefs.size();
    for (int i = 0, bound = thisDefs.size(); i < bound; i++)
      if (!shouldOverride(thisDefs.get(i), thatDefs.get(i))) return false;
    return true;
  }

  private static boolean shouldOverride(AttributeDef thisDef, AttributeDef thatDef) {
    assert thisDef.equals(thatDef);
    if (thisDef.id() == thatDef.id()) return false;
    assert thisDef.isIdentity() || thatDef.isIdentity();
    // should overrider if `thisDef` references `thatDef`
    return thisDef.referencesTo(thatDef.id());
  }
}
