package sjtu.ipads.wtune.stmt.resolver.internal;

import sjtu.ipads.wtune.common.attrs.FieldKey;
import sjtu.ipads.wtune.common.attrs.Fields;
import sjtu.ipads.wtune.sqlparser.ast.SQLNode;
import sjtu.ipads.wtune.sqlparser.multiversion.Catalog;
import sjtu.ipads.wtune.sqlparser.multiversion.CatalogBase;
import sjtu.ipads.wtune.stmt.resolver.BoolExpr;
import sjtu.ipads.wtune.stmt.resolver.BoolExprManager;

import java.util.Map;

public class BoolExprManagerImpl extends CatalogBase<SQLNode, BoolExpr> implements BoolExprManager {
  private BoolExprManagerImpl() {}

  private BoolExprManagerImpl(Map<SQLNode, Object> current, Catalog<SQLNode, BoolExpr> prev) {
    super(current, prev);
  }

  public static BoolExprManager build() {
    return new BoolExprManagerImpl();
  }

  @Override
  public BoolExpr bool(SQLNode node) {
    return get(node);
  }

  @Override
  public BoolExpr setBool(SQLNode node, BoolExpr expr) {
    return put(node, expr);
  }

  @Override
  protected Catalog<SQLNode, BoolExpr> makePrev(
      Map<SQLNode, Object> current, Catalog<SQLNode, BoolExpr> prev) {
    return new BoolExprManagerImpl(current, prev);
  }

  public static FieldKey<BoolExpr> fieldKey() {
    return BoolExprField.INSTANCE;
  }
}

class BoolExprField implements FieldKey<BoolExpr> {
  static FieldKey<BoolExpr> INSTANCE = new BoolExprField();

  private BoolExprField() {}

  @Override
  public String name() {
    return "stmt.boolExpr";
  }

  @Override
  public BoolExpr get(Fields owner) {
    final SQLNode node = owner.unwrap(SQLNode.class);
    return node.manager(BoolExprManager.class).bool(node);
  }

  @Override
  public BoolExpr set(Fields owner, BoolExpr obj) {
    final SQLNode node = owner.unwrap(SQLNode.class);
    return node.manager(BoolExprManager.class).setBool(node, obj);
  }
}
