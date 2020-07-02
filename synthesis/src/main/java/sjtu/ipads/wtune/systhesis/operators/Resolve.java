package sjtu.ipads.wtune.systhesis.operators;

import sjtu.ipads.wtune.sqlparser.SQLNode;
import sjtu.ipads.wtune.stmt.statement.Statement;

public class Resolve implements Operator {
  public static Operator build() {
    return new Resolve();
  }

  @Override
  public SQLNode apply(Statement stmt, SQLNode sqlNode) {
    stmt.parsed().relinkAll();
    stmt.reResolve();
    return sqlNode;
  }
}
