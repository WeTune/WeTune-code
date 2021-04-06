package sjtu.ipads.wtune.stmt.resolver;

import sjtu.ipads.wtune.common.attrs.FieldKey;
import sjtu.ipads.wtune.common.attrs.Fields;
import sjtu.ipads.wtune.sqlparser.ast.ASTNode;
import sjtu.ipads.wtune.sqlparser.ast.AttributeManagerBase;

class BoolExprManagerImpl extends AttributeManagerBase<BoolExpr> implements BoolExprManager {
  BoolExprManagerImpl() {
    super(true);
  }

  @Override
  public BoolExpr bool(ASTNode node) {
    return get(node);
  }

  @Override
  public BoolExpr setBool(ASTNode node, BoolExpr expr) {
    return set(node, expr);
  }

  public static FieldKey<BoolExpr> field() {
    return BoolExprField.INSTANCE;
  }

  @Override
  protected FieldKey<BoolExpr> fieldKey() {
    return BoolExprField.INSTANCE;
  }

  @Override
  public Class<?> key() {
    return BoolExprManager.class;
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
