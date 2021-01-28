package sjtu.ipads.wtune.stmt.resolver.internal;

import sjtu.ipads.wtune.common.attrs.FieldKey;
import sjtu.ipads.wtune.common.attrs.Fields;
import sjtu.ipads.wtune.sqlparser.ast.SQLNode;
import sjtu.ipads.wtune.sqlparser.multiversion.Catalog;
import sjtu.ipads.wtune.sqlparser.multiversion.CatalogBase;
import sjtu.ipads.wtune.stmt.resolver.Param;
import sjtu.ipads.wtune.stmt.resolver.ParamManager;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class ParamManagerImpl extends CatalogBase<SQLNode, Param> implements ParamManager {
  private ParamManagerImpl() {
    current = new HashMap<>();
  }

  private ParamManagerImpl(Map<SQLNode, Object> current, Catalog<SQLNode, Param> prev) {
    super(current, prev);
  }

  public static ParamManager build() {
    return new ParamManagerImpl();
  }

  @Override
  public Param param(SQLNode node) {
    return get(node);
  }

  @Override
  public Param setParam(SQLNode node, Param param) {
    return put(node, param);
  }

  @Override
  public Collection<Param> params() {
    return params0().values();
  }

  private Map<SQLNode, Param> params0() {
    final Map<SQLNode, Param> params;

    if (prev == null) params = new HashMap<>();
    else params = ((ParamManagerImpl) prev).params0();

    for (var pair : current.entrySet())
      if (pair.getValue() == REMOVED) params.remove(pair.getKey());
      else params.put(pair.getKey(), (Param) pair.getValue());

    return params;
  }

  @Override
  protected Catalog<SQLNode, Param> makePrev(
      Map<SQLNode, Object> current, Catalog<SQLNode, Param> prev) {
    return new ParamManagerImpl(current, prev);
  }

  public static FieldKey<Param> fieldKey() {
    return ParamField.INSTANCE;
  }
}

class ParamField implements FieldKey<Param> {
  static FieldKey<Param> INSTANCE = new ParamField();

  private ParamField() {}

  @Override
  public String name() {
    return "stmt.boolExpr";
  }

  @Override
  public Param get(Fields owner) {
    final SQLNode node = owner.unwrap(SQLNode.class);
    return node.manager(ParamManager.class).param(node);
  }

  @Override
  public Param set(Fields owner, Param obj) {
    final SQLNode node = owner.unwrap(SQLNode.class);
    return node.manager(ParamManager.class).setParam(node, obj);
  }
}
