package sjtu.ipads.wtune.prover.uexpr2;

record UNegImpl(UExpr body) implements UNeg {
  @Override
  public UExpr replaceBaseVar(UVar baseVar, UVar repVar) {
    final UExpr e = body.replaceBaseVar(baseVar, repVar);
    if (e != body) return UNeg.mk(e);
    else return this;
  }

  @Override
  public String toString() {
    return "not(" + body + ")";
  }
}
