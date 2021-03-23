package sjtu.ipads.wtune.stmt.resolver;

import java.util.IdentityHashMap;
import java.util.Map;
import sjtu.ipads.wtune.common.attrs.FieldKey;
import sjtu.ipads.wtune.common.attrs.Fields;
import sjtu.ipads.wtune.common.multiversion.Catalog;
import sjtu.ipads.wtune.common.multiversion.CatalogBase;
import sjtu.ipads.wtune.sqlparser.ast.ASTNode;

class BoolExprManagerImpl extends CatalogBase<ASTNode, BoolExpr> implements BoolExprManager {
  private BoolExprManagerImpl() {
    current = new IdentityHashMap<>();
  }

  private BoolExprManagerImpl(Map<ASTNode, Object> current, Catalog<ASTNode, BoolExpr> prev) {
    super(current, prev);
  }

  public static BoolExprManager build() {
    return new BoolExprManagerImpl();
  }

  @Override
  public BoolExpr bool(ASTNode node) {
    return get(node);
  }

  @Override
  public BoolExpr setBool(ASTNode node, BoolExpr expr) {
    return put(node, expr);
  }

  @Override
  protected Catalog<ASTNode, BoolExpr> makePrev(
      Map<ASTNode, Object> current, Catalog<ASTNode, BoolExpr> prev) {
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
    final ASTNode node = owner.unwrap(ASTNode.class);
    return node.manager(BoolExprManager.class).bool(node);
  }

  @Override
  public BoolExpr set(Fields owner, BoolExpr obj) {
    final ASTNode node = owner.unwrap(ASTNode.class);
    return node.manager(BoolExprManager.class).setBool(node, obj);
  }
}
