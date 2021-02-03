package sjtu.ipads.wtune.symsolver.core.impl;

import sjtu.ipads.wtune.symsolver.core.TableSym;

public class TableSymImpl extends BaseSym implements TableSym {
  public static TableSym build() {
    return new TableSymImpl();
  }

  @Override
  public String toString() {
    return "t" + index();
  }
}
