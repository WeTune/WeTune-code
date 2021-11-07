package sjtu.ipads.wtune.prover.uexpr2;

public interface UAdd extends UBinary {
  default UKind kind() {
    return UKind.ADD;
  }

  static UAdd mk(UExpr lhs, UExpr rhs) {
    return new UAddImpl(lhs, rhs);
  }
}
