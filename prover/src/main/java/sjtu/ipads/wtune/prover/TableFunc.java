package sjtu.ipads.wtune.prover;

public interface TableFunc extends UTerm {
  Name name();

  @Override
  default Kind kind() {
    return Kind.TABLE_FUNC;
  }
}
