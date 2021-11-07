package sjtu.ipads.wtune.prover.uexpr2;

public interface UMul extends UBinary {
  @Override
  default UKind kind() {
    return UKind.MUL;
  }

  static UMul mk(UExpr e0, UExpr e1) {
    return new UMulImpl(e0, e1);
  }
}
