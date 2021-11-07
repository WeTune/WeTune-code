package sjtu.ipads.wtune.prover.uexpr2;

public interface UBinary extends UExpr {
  UExpr lhs();

  UExpr rhs();
}