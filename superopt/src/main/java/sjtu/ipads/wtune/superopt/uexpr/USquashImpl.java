package sjtu.ipads.wtune.superopt.uexpr;

record USquashImpl(UTerm body) implements USquash {
  @Override
  public UTerm replaceBaseVar(UVar baseVar, UVar repVar) {
    final UTerm e = body.replaceBaseVar(baseVar, repVar);
    if (e != body) return USquash.mk(e);
    else return this;
  }

  @Override
  public String toString() {
    return "|" + body + "|";
  }
}
