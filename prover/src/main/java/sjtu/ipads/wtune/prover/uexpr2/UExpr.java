package sjtu.ipads.wtune.prover.uexpr2;

public interface UExpr {
  enum UKind {
    ATOM,
    MUL,
    ADD,
    SUM,
    NEG,
    SQUASH,
  }

  UKind kind();

  UExpr replaceBaseVar(UVar baseVar, UVar repVar);
}
