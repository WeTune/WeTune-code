package sjtu.ipads.wtune.prover.decision;

import java.util.List;
import sjtu.ipads.wtune.prover.expr.UExpr;
import sjtu.ipads.wtune.prover.utils.Util;

public class TableBijectionMatcher extends BijectionMatcher<UExpr> {
  protected TableBijectionMatcher(List<UExpr> xs, List<UExpr> ys) {
    super(xs, ys);
  }

  @Override
  protected boolean tryMatch(UExpr x, UExpr y) {
    return Util.compareTable(x, y);
  }
}
