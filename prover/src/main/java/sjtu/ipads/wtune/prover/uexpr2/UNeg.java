package sjtu.ipads.wtune.prover.uexpr2;

public interface UNeg extends UExpr {
  @Override
  default UKind kind() {
    return UKind.NEG;
  }

  UExpr body();

  static UExpr mk(UExpr body) {
    return new UNegImpl(body);
  }
}
