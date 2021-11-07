package sjtu.ipads.wtune.prover.uexpr2;

record USquashImpl(UExpr body) implements USquash {
  @Override
  public UExpr replaceBaseVar(UVar baseVar, UVar repVar) {
    final UExpr e = body.replaceBaseVar(baseVar, repVar);
    if (e != body) return USquash.mk(e);
    else return this;
  }

  @Override
  public String toString() {
    return "|" + body + "|";
  }
}
