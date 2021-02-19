package sjtu.ipads.wtune.superopt.fragment.symbolic.internal;

import sjtu.ipads.wtune.sqlparser.plan.OutputAttribute;
import sjtu.ipads.wtune.superopt.fragment.symbolic.AttributeInterpretation;

import java.util.List;

public class AttributeInterpretationImpl extends InterpretationBase<List<OutputAttribute>>
    implements AttributeInterpretation {

  public AttributeInterpretationImpl(List<OutputAttribute> projs) {
    super(projs);
  }

  @Override
  public boolean isCompatible(List<OutputAttribute> otherAttrs) {
    final List<OutputAttribute> attrs = this.object();
    if (otherAttrs.size() != attrs.size()) return false;
    for (int i = 0, bound = otherAttrs.size(); i < bound; i++)
      if (!otherAttrs.get(i).refEquals(attrs.get(i))) return false;
    return true;
  }
}
