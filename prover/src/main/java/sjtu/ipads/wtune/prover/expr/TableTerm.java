package sjtu.ipads.wtune.prover.expr;

public interface TableTerm extends UTerm {
  // Warning: DON'T use this class as Set or Map's key
  Name name();

  Tuple tuple();

  @Override
  default Kind kind() {
    return Kind.TABLE;
  }
}
