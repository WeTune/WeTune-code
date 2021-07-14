package sjtu.ipads.wtune.prover.uexpr;

public interface TableTerm extends UTerm {
  // Warning: DON'T use this class as Set or Map's key
  Name name();

  Var tuple();

  @Override
  default Kind kind() {
    return Kind.TABLE;
  }
}
