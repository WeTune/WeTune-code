package sjtu.ipads.wtune.superopt.fragment;

import sjtu.ipads.wtune.superopt.fragment.pruning.*;
import sjtu.ipads.wtune.superopt.util.Hole;

import java.util.List;
import java.util.Set;

import static sjtu.ipads.wtune.common.utils.IterableSupport.linearFind;
import static sjtu.ipads.wtune.common.utils.ListSupport.map;
import static sjtu.ipads.wtune.superopt.fragment.Op.mk;
import static sjtu.ipads.wtune.superopt.fragment.OpKind.*;

public class FragmentSupport {
  private static final int DEFAULT_MAX_OPS = 4;
  private static final List<Op> DEFAULT_OP_SET;
  private static final Set<Rule> BASIC_PRUNING_RULES;
  private static final Set<Rule> EXTENDED_PRUNING_RULES;

  static {
    DEFAULT_OP_SET =
        map(List.of(INNER_JOIN, LEFT_JOIN, SIMPLE_FILTER, PROJ, PROJ, IN_SUB_FILTER), Op::mk);
    BASIC_PRUNING_RULES =
        Set.of(new MalformedJoin(), new MalformedSubquery(), new NonLeftDeepJoin());
    EXTENDED_PRUNING_RULES =
        Set.of(
            new MalformedJoin(),
            new MalformedSubquery(),
            new NonLeftDeepJoin(),
            // There are some hard-coded optimization in Rewriter (TopDownOptimizer). Some rules
            // overlapped with those stuff. We preclude them here.
            new MeaninglessDedup(), // dedup in IN-subquery context
            // (since IN-sub enforce a set-semantic context)
            new TooManyJoin(), // more than 2 join (since we don't change join order)
            new ReorderedFilter(), // InSub before a Filter
            new TooManySimpleFilter(), // More than 2 simple filters
            new TooManySubqueryFilter(), // More than 2 subquery filters
            // (since filters in a chain can be freely reordered and combined)
            new TooManyProj() // More than 2 Proj (since they can be collapsed)
            );
    ((Proj) linearFind(DEFAULT_OP_SET, it -> it.kind() == PROJ)).setDeduplicated(true);
  }

  public static List<Fragment> enumFragments() {
    final FragmentEnumerator enumerator = new FragmentEnumerator(DEFAULT_OP_SET, DEFAULT_MAX_OPS);
    enumerator.setPruningRules(EXTENDED_PRUNING_RULES);
    return enumerator.enumerate();
  }

  /** Fill holes with Input operator and call setFragment on each operator. */
  public static Fragment setupFragment(Fragment fragment) {
    for (Hole<Op> hole : FragmentUtils.gatherHoles(fragment)) hole.fill(mk(INPUT));
    fragment.acceptVisitor(OpVisitor.traverse(it -> it.setFragment(fragment)));
    return fragment;
  }
}
