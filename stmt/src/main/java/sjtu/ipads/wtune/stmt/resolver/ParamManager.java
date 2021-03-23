package sjtu.ipads.wtune.stmt.resolver;

import sjtu.ipads.wtune.common.attrs.FieldKey;
import sjtu.ipads.wtune.sqlparser.ast.ASTNode;

import java.util.Collection;

public interface ParamManager {
  FieldKey<Param> PARAM = ParamManagerImpl.fieldKey();

  Param param(ASTNode node);

  Param setParam(ASTNode node, Param param);

  Collection<Param> params();

  static ParamManager build() {
    return ParamManagerImpl.build();
  }
}
