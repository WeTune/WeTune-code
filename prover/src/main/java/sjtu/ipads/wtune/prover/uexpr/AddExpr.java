package sjtu.ipads.wtune.prover.uexpr;

interface AddExpr extends UExpr {
  @Override
  default Kind kind() {
    return Kind.ADD;
  }
}
