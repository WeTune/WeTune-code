package sjtu.ipads.wtune.prover.expr;

public interface SumExpr extends UExpr {
  @Override
  default Kind kind() {
    return Kind.SUM;
  }
}
