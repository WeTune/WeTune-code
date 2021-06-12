package sjtu.ipads.wtune.prover.expr;

interface AddExpr extends UExpr {
  @Override
  default Kind kind() {
    return Kind.ADD;
  }
}
