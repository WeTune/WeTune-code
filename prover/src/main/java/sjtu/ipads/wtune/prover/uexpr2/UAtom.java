package sjtu.ipads.wtune.prover.uexpr2;

public interface UAtom extends UExpr {
  @Override
  default UKind kind() {
    return UKind.ATOM;
  }
}
