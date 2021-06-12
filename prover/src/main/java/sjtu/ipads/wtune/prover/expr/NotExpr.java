package sjtu.ipads.wtune.prover.expr;

public interface NotExpr extends UExpr {
  @Override
  default Kind kind() {
    return Kind.NOT;
  }
}
