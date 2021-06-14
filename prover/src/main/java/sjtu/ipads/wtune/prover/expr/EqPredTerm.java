package sjtu.ipads.wtune.prover.expr;

public interface EqPredTerm extends PredTerm {
  Tuple left();

  Tuple right();

  @Override
  default Kind kind() {
    return Kind.EQ_PRED;
  }
}
