package sjtu.ipads.wtune.stmt.resolver;

import sjtu.ipads.wtune.common.attrs.FieldKey;
import sjtu.ipads.wtune.sqlparser.ast.SQLNode;
import sjtu.ipads.wtune.stmt.resolver.internal.ParamManagerImpl;

import java.util.Collection;

public interface ParamManager {
  FieldKey<Param> PARAM = ParamManagerImpl.fieldKey();

  Param param(SQLNode node);

  Param setParam(SQLNode node, Param param);

  Collection<Param> params();

  static ParamManager build() {
    return ParamManagerImpl.build();
  }
}
