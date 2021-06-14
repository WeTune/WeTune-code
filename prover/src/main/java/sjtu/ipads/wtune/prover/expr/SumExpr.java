package sjtu.ipads.wtune.prover.expr;

import java.util.List;

public interface SumExpr extends UExpr {
  List<Tuple> boundTuples();

  @Override
  default Kind kind() {
    return Kind.SUM;
  }
}
