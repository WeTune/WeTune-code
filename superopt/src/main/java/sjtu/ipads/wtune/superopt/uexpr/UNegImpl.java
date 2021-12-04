package sjtu.ipads.wtune.superopt.uexpr;

record UNegImpl(UTerm body) implements UNeg {
  @Override
  public UTerm replaceBaseVar(UVar baseVar, UVar repVar) {
    final UTerm e = body.replaceBaseVar(baseVar, repVar);
    if (e != body) return UNeg.mk(e);
    else return this;
  }

  @Override
  public String toString() {
    return "not(" + body + ")";
  }
}
