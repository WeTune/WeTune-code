package sjtu.ipads.wtune.prover.expr;

import java.util.List;

public interface SumExpr extends UExpr {
  List<Tuple> boundedVars();

  @Override
  default Kind kind() {
    return Kind.SUM;
  }
}
