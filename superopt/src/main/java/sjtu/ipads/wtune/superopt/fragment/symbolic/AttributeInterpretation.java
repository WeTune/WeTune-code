package sjtu.ipads.wtune.superopt.fragment.symbolic;

import org.apache.commons.lang3.tuple.Pair;
import sjtu.ipads.wtune.sqlparser.plan.PlanAttribute;

import java.util.List;

public interface AttributeInterpretation
    extends Interpretation<Pair<List<PlanAttribute>, List<PlanAttribute>>> {
  default boolean isCompatible(List<PlanAttribute> used) {
    return isCompatible(Pair.of(used, null));
  }
}
