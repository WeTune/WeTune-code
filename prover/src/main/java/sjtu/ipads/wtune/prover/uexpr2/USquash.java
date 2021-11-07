package sjtu.ipads.wtune.prover.uexpr2;

public interface USquash extends UExpr {
  @Override
  default UKind kind() {
    return UKind.SQUASH;
  }

  UExpr body();

  static USquash mk(UExpr body) {
    return new USquashImpl(body);
  }
}
