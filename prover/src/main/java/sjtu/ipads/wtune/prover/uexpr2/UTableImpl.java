package sjtu.ipads.wtune.prover.uexpr2;

record UTableImpl(UName tableName, UVar var) implements UTable {
  @Override
  public UExpr replaceBaseVar(UVar baseVar, UVar repVar) {
    final UVar v = var.replaceBaseVar(baseVar, repVar);
    if (v != null) return UTable.mk(tableName, v);
    else return this;
  }

  @Override
  public String toString() {
    return tableName + "(" + var + ")";
  }
}
