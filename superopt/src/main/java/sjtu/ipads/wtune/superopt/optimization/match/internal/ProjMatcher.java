package sjtu.ipads.wtune.superopt.optimization.match.internal;

import sjtu.ipads.wtune.common.multiversion.Snapshot;
import sjtu.ipads.wtune.sqlparser.rel.Attribute;
import sjtu.ipads.wtune.superopt.optimization.match.Interpretations;
import sjtu.ipads.wtune.superopt.plan.Placeholder;
import sjtu.ipads.wtune.superopt.plan.Proj;
import sjtu.ipads.wtune.superopt.optimization.*;
import sjtu.ipads.wtune.superopt.optimization.match.MatchContext;
import sjtu.ipads.wtune.superopt.optimization.match.MatchResult;

import java.util.List;

import static sjtu.ipads.wtune.superopt.optimization.match.MatchResult.ABORT;

public class ProjMatcher extends MatcherBase {
  ProjMatcher(Proj proj) {
    super(proj);
  }

  public MatchResult match(MatchContext ctx, Operator target) {
    if (!(target instanceof ProjOp)) return ABORT;

    final List<Attribute> projection = ((ProjOp) target).projection();
    final Placeholder placeholder = ((Proj) planNode()).fields();

    // take snapshot
    final Interpretations interpretations = ctx.interpretations();
    final Snapshot snapshot = interpretations.snapshot();
    interpretations.derive();

    // assign projection to placeholder
    if (interpretations.assignProjection(placeholder, projection))
      // percolate down to predecessor
      return matcher(planNode.predecessors()[0]).match(ctx, target.predecessors()[0]);
    else {
      // rollback and ask caller to retry
      interpretations.setSnapshot(snapshot);
      return MatchResult.RETRY;
    }
  }
}
