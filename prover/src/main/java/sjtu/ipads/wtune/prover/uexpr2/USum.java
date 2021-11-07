package sjtu.ipads.wtune.prover.uexpr2;

public interface USum extends UExpr {
  @Override
  default UKind kind() {
    return UKind.SUM;
  }

  UVar[] sumVars();

  UExpr body();

  static USum mk(UVar[] sumVars, UExpr body) {
    return new USumImpl(sumVars, body);
  }
}
