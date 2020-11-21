package sjtu.ipads.wtune.superopt;

import sjtu.ipads.wtune.superopt.rules.Rule;
import sjtu.ipads.wtune.superopt.rules.simplify.DoubleProj;
import sjtu.ipads.wtune.superopt.rules.support.AllJoinRule;
import sjtu.ipads.wtune.superopt.rules.support.AllUnionRule;
import sjtu.ipads.wtune.superopt.rules.validation.*;

public class Heuristic {
  public static final int SKELETON_MAX_OPS = 4;

  public static boolean pruneSkeleton(Graph graph) {
    return !Rule.match(MalformedDistinct.class, graph)
        && !Rule.match(MalformedSort.class, graph)
        && !Rule.match(MalformedJoin.class, graph)
        && !Rule.match(MalformedLimit.class, graph)
        && !Rule.match(MalformedUnion.class, graph)
        && !Rule.match(DoubleProj.class, graph)
        && !Rule.match(AllUnionRule.class, graph)
        && !Rule.match(AllJoinRule.class, graph);
  }
}
