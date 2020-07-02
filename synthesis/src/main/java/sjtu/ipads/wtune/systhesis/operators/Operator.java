package sjtu.ipads.wtune.systhesis.operators;

import sjtu.ipads.wtune.sqlparser.SQLNode;
import sjtu.ipads.wtune.stmt.statement.Statement;

public interface Operator {
  default SQLNode apply(SQLNode sqlNode) {
    return apply(null, sqlNode);
  }

  default SQLNode apply(Statement stmt, SQLNode sqlNode) {
    return apply(sqlNode);
  }

  default SQLNode apply(Statement stmt) {
    return apply(stmt, stmt.parsed());
  }
}
