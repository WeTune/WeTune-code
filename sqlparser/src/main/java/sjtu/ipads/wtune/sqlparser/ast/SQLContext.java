package sjtu.ipads.wtune.sqlparser.ast;

import sjtu.ipads.wtune.sqlparser.ast.internal.NodeMgr;
import sjtu.ipads.wtune.sqlparser.ast.internal.SQLContextImpl;
import sjtu.ipads.wtune.sqlparser.rel.Schema;

public interface SQLContext {
  interface Snapshot {
    NodeMgr key();
  }

  String dbType();

  Snapshot snapshot();

  void derive();

  void setSnapshot(Snapshot snapshot);

  Schema schema();

  void setSchema(Schema schema);

  static SQLNode manage(String dbType, SQLNode node) {
    return SQLContextImpl.build(dbType).manage(node);
  }
}
