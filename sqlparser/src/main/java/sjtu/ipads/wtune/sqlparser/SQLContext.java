package sjtu.ipads.wtune.sqlparser;

import sjtu.ipads.wtune.sqlparser.ast.SQLNode;
import sjtu.ipads.wtune.sqlparser.ast.internal.NodeMgr;
import sjtu.ipads.wtune.sqlparser.ast.internal.SQLContextImpl;
import sjtu.ipads.wtune.sqlparser.rel.Relation;
import sjtu.ipads.wtune.sqlparser.rel.Schema;

public interface SQLContext {
  System.Logger LOG = System.getLogger("wetune.sqlparser");

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

  static SQLNode resolveRelation(SQLNode node) {
    return resolveRelation(node, node.context().schema());
  }

  static SQLNode resolveRelation(SQLNode node, Schema schema) {
    if (schema == null) throw new IllegalArgumentException("schema not set");
    node.context().setSchema(schema);
    Relation.resolve(node);
    return node;
  }
}
