package sjtu.ipads.wtune.symsolver.core.impl;

import sjtu.ipads.wtune.symsolver.core.Scoped;
import sjtu.ipads.wtune.symsolver.core.TableSym;

public class TableSymImpl extends BaseSym implements TableSym {
  private TableSymImpl(Scoped scoped) {
    super(scoped);
  }

  public static TableSym build(Scoped scoped) {
    return new TableSymImpl(scoped);
  }

  @Override
  public String toString() {
    return "t" + index();
  }
}
