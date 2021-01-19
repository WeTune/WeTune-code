package sjtu.ipads.wtune.symsolver.utils;

import sjtu.ipads.wtune.symsolver.core.Scoped;

public class SimpleScoped implements Scoped {
  private final Object scope;

  public SimpleScoped(Object scope) {
    this.scope = scope;
  }

  public SimpleScoped() {
    this.scope = new Object();
  }

  @Override
  public Object scope() {
    return scope;
  }
}
