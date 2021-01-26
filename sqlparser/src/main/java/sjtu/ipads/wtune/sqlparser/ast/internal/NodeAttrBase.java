package sjtu.ipads.wtune.sqlparser.ast.internal;

import sjtu.ipads.wtune.common.attrs.AttrKey;
import sjtu.ipads.wtune.common.attrs.Attrs;
import sjtu.ipads.wtune.sqlparser.ast.SQLNode;

import java.util.Arrays;

import static java.lang.System.Logger.Level.WARNING;
import static sjtu.ipads.wtune.sqlparser.SQLContext.LOG;

public abstract class NodeAttrBase<T> implements AttrKey<T> {
  private final String name;
  private final Class<?> targetClass;

  protected NodeAttrBase(String name, Class<?> targetClass) {
    this.name = name;
    this.targetClass = targetClass;
  }

  @Override
  public String name() {
    return name;
  }

  @Override
  public boolean validate(Attrs owner, Object obj) {
    return owner instanceof SQLNode && targetClass.isInstance(obj);
  }

  @Override
  public T rescue(Attrs owner, Object obj) {
    LOG.log(
        WARNING,
        "mis-typed SQLNode attribute: {0} for {1}. stacktrace:\n{2}",
        obj,
        name,
        Arrays.asList(Thread.currentThread().getStackTrace()));
    return null;
  }

  @Override
  public String toString() {
    return name;
  }
}
