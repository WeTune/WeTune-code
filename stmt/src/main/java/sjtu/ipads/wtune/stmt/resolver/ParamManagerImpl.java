package sjtu.ipads.wtune.stmt.resolver;

import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.Map;
import sjtu.ipads.wtune.common.attrs.FieldKey;
import sjtu.ipads.wtune.common.attrs.Fields;
import sjtu.ipads.wtune.common.multiversion.Catalog;
import sjtu.ipads.wtune.common.multiversion.CatalogBase;
import sjtu.ipads.wtune.sqlparser.ast.ASTNode;

class ParamManagerImpl extends CatalogBase<ASTNode, Param> implements ParamManager {
  private ParamManagerImpl() {
    current = new IdentityHashMap<>();
  }

  private ParamManagerImpl(Map<ASTNode, Object> current, Catalog<ASTNode, Param> prev) {
    super(current, prev);
  }

  public static ParamManager build() {
    return new ParamManagerImpl();
  }

  @Override
  public Param param(ASTNode node) {
    return get(node);
  }

  @Override
  public Param setParam(ASTNode node, Param param) {
    return put(node, param);
  }

  @Override
  public Collection<Param> params() {
    return params0().values();
  }

  private Map<ASTNode, Param> params0() {
    final Map<ASTNode, Param> params;

    if (prev == null) params = new IdentityHashMap<>();
    else params = ((ParamManagerImpl) prev).params0();

    for (var pair : current.entrySet())
      if (pair.getValue() == REMOVED) params.remove(pair.getKey());
      else params.put(pair.getKey(), (Param) pair.getValue());

    return params;
  }

  @Override
  protected Catalog<ASTNode, Param> makePrev(
      Map<ASTNode, Object> current, Catalog<ASTNode, Param> prev) {
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
    final ASTNode node = owner.unwrap(ASTNode.class);
    return node.manager(ParamManager.class).param(node);
  }

  @Override
  public Param set(Fields owner, Param obj) {
    final ASTNode node = owner.unwrap(ASTNode.class);
    return node.manager(ParamManager.class).setParam(node, obj);
  }
}
