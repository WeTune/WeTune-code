package sjtu.ipads.wtune.symsolver.core.impl;

import sjtu.ipads.wtune.symsolver.core.Sym;
import sjtu.ipads.wtune.symsolver.logic.Func;

public abstract class BaseSym implements Sym {
  private Object scope;
  private int index;
  private Func func;

  protected BaseSym() {
    this.index = -1;
  }

  @Override
  public void setScope(Object scope) {
    this.scope = scope;
  }

  @Override
  public int index() {
    return index;
  }

  @Override
  public void setIndex(int index) {
    if (isIndexed()) throw new IllegalStateException("index shouldn't be changed");
    this.index = index;
  }

  @Override
  public Func func() {
    return func;
  }

  @Override
  public void setFunc(Func func) {
    this.func = func;
  }

  @Override
  public Object scope() {
    return scope;
  }
}
