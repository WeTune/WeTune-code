package sjtu.ipads.wtune.symsolver.core.impl;

import sjtu.ipads.wtune.symsolver.core.PickSym;
import sjtu.ipads.wtune.symsolver.core.Query;
import sjtu.ipads.wtune.symsolver.core.TableSym;

public abstract class BaseQuery implements Query {
  private String name;

  protected final TableSym[] tables;
  protected final PickSym[] picks;

  protected BaseQuery(int nTbls, int nPicks) {
    tables = makeTableSyms(nTbls);
    picks = makePickSyms(nPicks);
  }

  private static TableSym[] makeTableSyms(int n) {
    final TableSym[] tables = new TableSym[n];
    for (int i = 0; i < n; i++) tables[i] = TableSym.from(i);
    return tables;
  }

  private static PickSym[] makePickSyms(int n) {
    final PickSym[] picks = new PickSym[n];
    for (int i = 0; i < n; i++) picks[i] = PickSym.from(i);
    return picks;
  }

  @Override
  public Query setName(String name) {
    this.name = name;
    return this;
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
