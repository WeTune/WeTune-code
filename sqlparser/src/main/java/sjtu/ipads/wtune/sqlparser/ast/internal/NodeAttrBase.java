package sjtu.ipads.wtune.sqlparser.ast.internal;

import sjtu.ipads.wtune.common.attrs.AttrKey;
import sjtu.ipads.wtune.common.attrs.Attrs;
import sjtu.ipads.wtune.sqlparser.ast.AttributeManager;
import sjtu.ipads.wtune.sqlparser.ast.SQLNode;

import java.util.function.Supplier;

import static java.lang.System.Logger.Level.WARNING;
import static sjtu.ipads.wtune.common.utils.FuncUtils.listMap;
import static sjtu.ipads.wtune.sqlparser.SQLContext.LOG;
import static sjtu.ipads.wtune.sqlparser.ast.NodeAttr.PARENT;

public class NodeAttrBase<T> implements AttrKey<T> {
  public static final String SQL_ATTR_PREFIX = "sql.attr.";

  private final String name;
  private final Class<?> targetClass;

  protected NodeAttrBase(String name, Class<?> targetClass) {
    this.name = name;
    this.targetClass = targetClass;
  }

  public static <T> AttrKey<T> build(String name, Class<?> targetClass) {
    return new NodeAttrBase<>(SQL_ATTR_PREFIX + name, targetClass);
  }

  @Override
  public T get(Attrs owner) {
    final SQLNode node = owner.unwrap(SQLNode.class);
    final AttributeManager mgr = node.attrMgr();

    if (mgr != null) return mgr.getAttr(node, this);
    else return AttrKey.super.get(node);
  }

  @Override
  public T set(Attrs owner, T obj) {
    final SQLNode node = owner.unwrap(SQLNode.class);
    final AttributeManager mgr = node.attrMgr();

    if (this != PARENT) SQLNode.setParent(obj, node);

    if (mgr != null) return mgr.setAttr(node, this, obj);
    else return AttrKey.super.set(node, obj);
  }

  @Override
  public T unset(Attrs owner) {
    final SQLNode node = owner.unwrap(SQLNode.class);
    final AttributeManager mgr = node.attrMgr();

    if (mgr != null) return mgr.unsetAttr(node, this);
    else return AttrKey.super.unset(node);
  }

  @Override
  public T setIfAbsent(Attrs owner, T obj) {
    final T t = get(owner);
    if (t != null) return t;

    set(owner, obj);
    return obj;
  }

  @Override
  public T setIfAbsent(Attrs owner, Supplier<T> supplier) {
    final T t = get(owner);
    if (t != null) return t;

    final T newT = supplier.get();
    set(owner, newT);
    return newT;
  }

  @Override
  public String name() {
    return name;
  }

  @Override
  public boolean validate(Attrs owner, Object obj) {
    return owner instanceof SQLNode && (obj == null || targetClass.isInstance(obj));
  }

  @Override
  public T rescue(Attrs owner, Object obj) {
    LOG.log(
        WARNING,
        "mis-typed SQLNode attribute: {0} for {1} \nStacktrace:\n  {2}",
        obj,
        name,
        String.join("\n  ", listMap(Object::toString, Thread.currentThread().getStackTrace())));
    return null;
  }

  @Override
  public String toString() {
    return name;
  }
}
