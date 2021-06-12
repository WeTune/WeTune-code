package sjtu.ipads.wtune.prover.expr;

public interface TableTerm extends UTerm {
  Name name();

  @Override
  default Kind kind() {
    return Kind.TABLE;
  }
}
