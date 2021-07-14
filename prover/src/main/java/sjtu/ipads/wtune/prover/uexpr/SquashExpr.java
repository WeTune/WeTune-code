package sjtu.ipads.wtune.prover.uexpr;

public interface SquashExpr extends UExpr {
  @Override
  default Kind kind() {
    return Kind.SQUASH;
  }
}
