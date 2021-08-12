package sjtu.ipads.wtune.prover.uexpr;

public interface NotExpr extends UExpr {
  @Override
  default Kind kind() {
    return Kind.NOT;
  }
}
