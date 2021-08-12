package sjtu.ipads.wtune.prover.uexpr;

public interface UninterpretedPredTerm extends PredTerm {
  Name name();

  Var[] vars();

  @Override
  default Kind kind() {
    return Kind.PRED;
  }
}
