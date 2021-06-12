package sjtu.ipads.wtune.prover.expr;

public interface MulExpr extends UExpr {
  @Override
  default Kind kind() {
    return Kind.MUL;
  }
}
