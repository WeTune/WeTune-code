package sjtu.ipads.wtune.prover.uexpr;

import java.util.List;

public interface SumExpr extends UExpr {
  List<Var> boundedVars();

  @Override
  default Kind kind() {
    return Kind.SUM;
  }
}
