package sjtu.ipads.wtune.stmt.resolver;

import java.util.Collection;
import sjtu.ipads.wtune.common.attrs.FieldKey;
import sjtu.ipads.wtune.sqlparser.ast.ASTNode;
import sjtu.ipads.wtune.sqlparser.ast.AttributeManager;

public interface ParamManager extends AttributeManager<Param> {
  FieldKey<Param> PARAM = ParamManagerImpl.field();

  Param param(ASTNode node);

  Param setParam(ASTNode node, Param param);

  Collection<Param> params();

  static ParamManager build() {
    return new ParamManagerImpl();
  }
}
