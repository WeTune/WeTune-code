package sjtu.ipads.wtune.superopt.fragment;

import sjtu.ipads.wtune.superopt.fragment.pruning.*;

import java.util.List;
import java.util.Set;

import static sjtu.ipads.wtune.common.utils.IterableSupport.linearFind;
import static sjtu.ipads.wtune.common.utils.ListSupport.map;
import static sjtu.ipads.wtune.superopt.fragment.OpKind.*;

public class FragmentSupport {
  private static final int DEFAULT_MAX_OPS = 4;
  private static final List<Op> DEFAULT_OP_SET =
      map(List.of(INNER_JOIN, LEFT_JOIN, SIMPLE_FILTER, PROJ, PROJ, IN_SUB_FILTER), Op::mk);
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
    ((Proj) linearFind(DEFAULT_OP_SET, it -> it.kind() == PROJ)).setDeduplicated(true);
  }

  public static List<Fragment> enumFragments() {
    final FragmentEnumerator enumerator = new FragmentEnumerator(DEFAULT_OP_SET, DEFAULT_MAX_OPS);
    enumerator.setPruningRules(DEFAULT_PRUNING_RULES);
    return enumerator.enumerate();
  }
}
