package sjtu.ipads.wtune.prover.expr;

public interface UninterpretedPredTerm extends PredTerm {
  Name name();

  Tuple[] tuple();

  @Override
  default Kind kind() {
    return Kind.PRED;
  }
}
