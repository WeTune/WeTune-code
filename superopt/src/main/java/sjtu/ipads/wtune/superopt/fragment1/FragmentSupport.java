package sjtu.ipads.wtune.superopt.fragment1;

import org.apache.commons.lang3.tuple.Pair;
import sjtu.ipads.wtune.sqlparser.plan1.PlanNode;
import sjtu.ipads.wtune.superopt.constraint.Constraints;
import sjtu.ipads.wtune.superopt.fragment1.pruning.*;
import sjtu.ipads.wtune.superopt.substitution.Substitution;

import java.util.List;
import java.util.Set;

import static sjtu.ipads.wtune.common.utils.FuncUtils.find;
import static sjtu.ipads.wtune.common.utils.FuncUtils.listMap;
import static sjtu.ipads.wtune.sqlparser.plan.OperatorType.*;

public class FragmentSupport {
  private static final int DEFAULT_MAX_OPS = 4;
  private static final List<Op> DEFAULT_OP_SET =
      listMap(List.of(INNER_JOIN, LEFT_JOIN, SIMPLE_FILTER, PROJ, PROJ, IN_SUB_FILTER), Op::mk);
  private static final Set<Rule> DEFAULT_PRUNING_RULES =
      Set.of(
          new MalformedJoin(),
          new MalformedSubquery(),
          new NonLeftDeepJoin(),
          new TooManyJoin(),
          new TooManySubqueryFilter(),
          new TooManySimpleFilter(),
          new TooManyProj(),
          new ReorderedFilter(),
          new MeaninglessDedup());

  static {
    ((Proj) find(FragmentSupport.DEFAULT_OP_SET, it -> it.kind() == PROJ)).setDeduplicated(true);
  }

  public static List<Fragment> enumFragments() {
    final FragmentEnumerator enumerator = new FragmentEnumerator(DEFAULT_OP_SET, DEFAULT_MAX_OPS);
    enumerator.setPruningRules(DEFAULT_PRUNING_RULES);
    return enumerator.enumerate();
  }

  public static PlanNode translateAsPlan(Fragment fragment, Constraints constraints) {
    return new PlanTranslator().translate(fragment, constraints);
  }

  public static Pair<PlanNode, PlanNode> translateAsPlan(
      Substitution substitution, boolean backwardCompatible) {
    return new PlanTranslator().translate(substitution, backwardCompatible);
  }

  public static Complexity calcComplexity(Fragment fragment) {
    return new ComplexityImpl(fragment);
  }
}
