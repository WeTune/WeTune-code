package sjtu.ipads.wtune.prover.expr;

public interface SquashExpr extends UExpr {
  @Override
  default Kind kind() {
    return Kind.SQUASH;
  }
}
