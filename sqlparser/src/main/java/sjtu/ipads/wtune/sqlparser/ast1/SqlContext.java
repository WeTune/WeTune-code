package sjtu.ipads.wtune.sqlparser.ast1;

import sjtu.ipads.wtune.common.tree.AstContext;
import sjtu.ipads.wtune.sqlparser.schema.Schema;

public interface SqlContext extends AstContext<SqlKind> {
  Schema schema();

  String dbType();

  void setSchema(Schema schema);

  void setDbType(String dbType);

  static SqlContext mk(int expectedNumNodes) {
    return new SqlContextImpl(expectedNumNodes, null);
  }
}
