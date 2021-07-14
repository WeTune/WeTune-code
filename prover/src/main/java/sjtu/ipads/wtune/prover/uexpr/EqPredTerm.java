package sjtu.ipads.wtune.prover.uexpr;

public interface EqPredTerm extends PredTerm {
  Var left();

  Var right();

  @Override
  default Kind kind() {
    return Kind.EQ_PRED;
  }
}
