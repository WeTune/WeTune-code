package sjtu.ipads.wtune.prover.uexpr;

public interface MulExpr extends UExpr {
  @Override
  default Kind kind() {
    return Kind.MUL;
  }
}
