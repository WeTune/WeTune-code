package sjtu.ipads.wtune.superopt.fragment.symbolic.internal;

import sjtu.ipads.wtune.superopt.fragment.symbolic.Interpretation;

public abstract class InterpretationBase<T> implements Interpretation<T> {
  private final T object;

  protected InterpretationBase(T object) {
    this.object = object;
  }

  @Override
  public T object() {
    return object;
  }
}
