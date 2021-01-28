package sjtu.ipads.wtune.sqlparser.ast.internal;

import sjtu.ipads.wtune.common.attrs.FieldKey;
import sjtu.ipads.wtune.common.attrs.Fields;
import sjtu.ipads.wtune.sqlparser.ast.FieldManager;
import sjtu.ipads.wtune.sqlparser.ast.SQLNode;
import sjtu.ipads.wtune.sqlparser.rel.Relation;

import java.util.function.Supplier;

import static java.lang.System.Logger.Level.WARNING;
import static sjtu.ipads.wtune.common.utils.FuncUtils.listMap;
import static sjtu.ipads.wtune.sqlparser.SQLContext.LOG;
import static sjtu.ipads.wtune.sqlparser.ast.NodeFields.PARENT;
import static sjtu.ipads.wtune.sqlparser.rel.Relation.RELATION;

public class NodeFieldBase<T> implements FieldKey<T> {
  public static final String SQL_ATTR_PREFIX = "sql.attr.";

  private final String name;
  private final Class<?> targetClass;

  protected NodeFieldBase(String name, Class<?> targetClass) {
    this.name = name;
    this.targetClass = targetClass;
  }

  public static <T> FieldKey<T> build(String name, Class<?> targetClass) {
    return new NodeFieldBase<>(SQL_ATTR_PREFIX + name, targetClass);
  }

  @Override
  public T get(Fields owner) {
    final SQLNode node = owner.unwrap(SQLNode.class);
    final FieldManager mgr = node.attrMgr();

    if (mgr != null) return mgr.getField(node, this);
    else return FieldKey.super.get(node);
  }

  @Override
  public T set(Fields owner, T obj) {
    final SQLNode node = owner.unwrap(SQLNode.class);
    final FieldManager mgr = node.attrMgr();

    final T old;
    if (mgr != null) old = mgr.setField(node, this, obj);
    else old = FieldKey.super.set(node, obj);

    if (this == PARENT) return old;

    SQLNode.setParent(obj, node);
    SQLNode.setContext(obj, node.context());
    if (mgr != null && (isRelationBoundary(old) || isRelationBoundary(obj)))
      node.get(RELATION).reset();

    return old;
  }

  @Override
  public T unset(Fields owner) {
    final SQLNode node = owner.unwrap(SQLNode.class);
    final FieldManager mgr = node.attrMgr();

    final T old;
    if (mgr != null) old = mgr.unsetField(node, this);
    else old = FieldKey.super.unset(node);

    return old;
  }

  @Override
  public T setIfAbsent(Fields owner, T obj) {
    final T t = get(owner);
    if (t != null) return t;

    set(owner, obj);
    return obj;
  }

  @Override
  public T setIfAbsent(Fields owner, Supplier<T> supplier) {
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
  public boolean validate(Fields owner, Object obj) {
    return owner instanceof SQLNode && (obj == null || targetClass.isInstance(obj));
  }

  @Override
  public T rescue(Fields owner, Object obj) {
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

  private static boolean isRelationBoundary(Object obj) {
    return obj instanceof SQLNode && Relation.isRelationBoundary((SQLNode) obj);
  }
}