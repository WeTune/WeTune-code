package sjtu.ipads.wtune.prover.uexpr;

public interface EqPredTerm extends PredTerm {
  Var lhs();

  Var rhs();

  @Override
  default Kind kind() {
    return Kind.EQ_PRED;
  }
}
