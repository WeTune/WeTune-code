package sjtu.ipads.wtune.superopt.optimization.match.internal;

import sjtu.ipads.wtune.common.multiversion.Snapshot;
import sjtu.ipads.wtune.superopt.plan.Input;
import sjtu.ipads.wtune.superopt.optimization.Operator;
import sjtu.ipads.wtune.superopt.optimization.match.Interpretations;
import sjtu.ipads.wtune.superopt.optimization.match.MatchContext;
import sjtu.ipads.wtune.superopt.optimization.match.MatchResult;

import static sjtu.ipads.wtune.superopt.optimization.match.MatchResult.RETRY;
import static sjtu.ipads.wtune.superopt.optimization.match.MatchResult.SUCCEED;

public class InputMatcher extends MatcherBase {
  InputMatcher(Input input) {
    super(input);
  }

  @Override
  public MatchResult match(MatchContext ctx, Operator operator) {
    final Interpretations interpretations = ctx.interpretations();
    final Snapshot snapshot = interpretations.snapshot();
    interpretations.derive();

    if (interpretations.assignInput(((Input) planNode).table(), operator)) return SUCCEED;
    else {
      interpretations.setSnapshot(snapshot);
      return RETRY;
    }
  }
}
