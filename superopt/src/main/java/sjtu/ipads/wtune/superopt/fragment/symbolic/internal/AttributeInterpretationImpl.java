package sjtu.ipads.wtune.superopt.fragment.symbolic.internal;

import sjtu.ipads.wtune.sqlparser.plan.PlanAttribute;
import sjtu.ipads.wtune.superopt.fragment.symbolic.AttributeInterpretation;

import java.util.List;

public class AttributeInterpretationImpl extends InterpretationBase<List<PlanAttribute>>
    implements AttributeInterpretation {

  public AttributeInterpretationImpl(List<PlanAttribute> projs) {
    super(projs);
  }

  @Override
  public boolean isCompatible(List<PlanAttribute> otherAttrs) {
    final List<PlanAttribute> attrs = this.object();
    if (otherAttrs.size() != attrs.size()) return false;
    for (int i = 0, bound = otherAttrs.size(); i < bound; i++)
      if (!otherAttrs.get(i).refEquals(attrs.get(i))) return false;
    return true;
  }
}
