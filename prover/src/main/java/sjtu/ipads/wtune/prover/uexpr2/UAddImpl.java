package sjtu.ipads.wtune.prover.uexpr2;

record UAddImpl(UExpr lhs, UExpr rhs) implements UAdd {
  @Override
  public UExpr replaceBaseVar(UVar baseVar, UVar repVar) {
    final UExpr l = lhs.replaceBaseVar(baseVar, repVar);
    final UExpr r = rhs.replaceBaseVar(baseVar, repVar);
    if (l != lhs || r != rhs) return UAdd.mk(l, r);
    else return this;
  }

  @Override
  public String toString() {
    return lhs + " + " + rhs;
  }
}
