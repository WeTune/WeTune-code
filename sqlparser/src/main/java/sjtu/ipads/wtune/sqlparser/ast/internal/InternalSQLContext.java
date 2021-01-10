package sjtu.ipads.wtune.sqlparser.ast.internal;

import sjtu.ipads.wtune.sqlparser.ast.SQLContext;
import sjtu.ipads.wtune.sqlparser.ast.SQLNode;

import java.util.List;

import static sjtu.ipads.wtune.common.utils.FuncUtils.listMap;

public interface InternalSQLContext extends SQLContext, NodeMapping {
  int version();

  SQLNode manage(SQLNode node);

  default List<SQLNode> manage(Iterable<SQLNode> nodes) {
    return listMap(this::manage, nodes);
  }

  static InternalSQLContext ofDbType(String dbType) {
    return SQLContextImpl.build(dbType);
  }
}
