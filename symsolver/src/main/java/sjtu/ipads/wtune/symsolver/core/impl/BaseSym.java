package sjtu.ipads.wtune.symsolver.core.impl;

import sjtu.ipads.wtune.symsolver.core.Query;
import sjtu.ipads.wtune.symsolver.core.Sym;
import sjtu.ipads.wtune.symsolver.logic.Func;

public abstract class BaseSym implements Sym {
  private final Query owner;
  private final Object wrapped;
  private int index;
  private Func func;

  protected BaseSym(Query owner, Object wrapped) {
    this.owner = owner;
    this.wrapped = wrapped;
    this.index = -1;
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
    return owner;
  }

  @Override
  public <T> T unwrap(Class<T> cls) {
    return (T) wrapped;
  }
}
