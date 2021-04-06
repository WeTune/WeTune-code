package sjtu.ipads.wtune.stmt.resolver;

import java.util.Collection;
import sjtu.ipads.wtune.common.attrs.FieldKey;
import sjtu.ipads.wtune.common.attrs.Fields;
import sjtu.ipads.wtune.sqlparser.ast.ASTNode;
import sjtu.ipads.wtune.sqlparser.ast.AttributeManagerBase;

class ParamManagerImpl extends AttributeManagerBase<Param> implements ParamManager {
  ParamManagerImpl() {
    super(true);
  }

  @Override
  public Param param(ASTNode node) {
    return get(node);
  }

  @Override
  public Param setParam(ASTNode node, Param param) {
    return set(node, param);
  }

  @Override
  public Collection<Param> params() {
    return attributes().values();
  }

  public static FieldKey<Param> field() {
    return ParamField.INSTANCE;
  }

  public FieldKey<Param> fieldKey() {
    return ParamField.INSTANCE;
  }

  @Override
  public Class<?> key() {
    return ParamManager.class;
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
