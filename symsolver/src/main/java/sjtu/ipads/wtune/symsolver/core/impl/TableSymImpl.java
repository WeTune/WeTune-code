package sjtu.ipads.wtune.symsolver.core.impl;

import sjtu.ipads.wtune.symsolver.core.Query;
import sjtu.ipads.wtune.symsolver.core.TableSym;

public class TableSymImpl extends BaseSym implements TableSym {
  private TableSymImpl(Query owner, Object wrapped) {
    super(owner, wrapped);
  }

  public static TableSym build(Query owner, Object wrapped) {
    return new TableSymImpl(owner, wrapped);
  }

  @Override
  public String toString() {
    return "t" + index();
  }
}
