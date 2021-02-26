package sjtu.ipads.wtune.superopt.fragment.symbolic.internal;

import org.apache.commons.lang3.tuple.Pair;
import sjtu.ipads.wtune.sqlparser.plan.PlanAttribute;
import sjtu.ipads.wtune.superopt.fragment.symbolic.AttributeInterpretation;

import java.util.List;
import java.util.Objects;

public class AttributeInterpretationImpl
    extends InterpretationBase<Pair<List<PlanAttribute>, List<PlanAttribute>>>
    implements AttributeInterpretation {

  protected AttributeInterpretationImpl(List<PlanAttribute> in, List<PlanAttribute> out) {
    super(Pair.of(in, out));
  }

  @Override
  public boolean isCompatible(Pair<List<PlanAttribute>, List<PlanAttribute>> obj) {
    final List<PlanAttribute> used0 = object().getLeft(), used1 = obj.getLeft();

    if (used0.size() != used1.size()) return false;

    for (int i = 0, bound = used0.size(); i < bound; i++)
      if (!Objects.equals(used0.get(i), used1.get(i))) return false;
    return true;
  }

  @Override
  public boolean shouldOverride(Pair<List<PlanAttribute>, List<PlanAttribute>> obj) {
    return obj.getRight() != null && object().getRight() == null;
  }
}
