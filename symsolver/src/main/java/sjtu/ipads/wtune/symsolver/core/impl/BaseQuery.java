package sjtu.ipads.wtune.symsolver.core.impl;

import sjtu.ipads.wtune.symsolver.core.PickSym;
import sjtu.ipads.wtune.symsolver.core.Query;
import sjtu.ipads.wtune.symsolver.core.TableSym;

import static sjtu.ipads.wtune.common.utils.FuncUtils.generate;

public abstract class BaseQuery implements Query {
  private String name;

  protected final TableSym[] tables;
  protected final PickSym[] picks;

  protected BaseQuery(int nTbls, int nPicks) {
    tables = generate(nTbls, TableSym.class, i -> TableSym.from(this, i));
    picks = generate(nPicks, PickSym.class, i -> PickSym.from(this, i));
  }

  @Override
  public void setName(String name) {
    this.name = name;
  }

  @Override
  public String name() {
    return name;
  }

  @Override
  public TableSym[] tables() {
    return tables;
  }

  @Override
  public PickSym[] picks() {
    return picks;
  }
}
