package sjtu.ipads.wtune.sqlparser.ast;

import sjtu.ipads.wtune.sqlparser.ast.internal.NodeMapping;
import sjtu.ipads.wtune.sqlparser.ast.internal.SQLContextImpl;

public interface SQLContext {
  interface Snapshot {
    int version();

    NodeMapping key();
  }

  String dbType();

  Snapshot snapshot();

  void derive();

  void setSnapshot(Snapshot snapshot);

  static SQLContext ofDbType(String dbType) {
    return SQLContextImpl.build(dbType);
  }
}
