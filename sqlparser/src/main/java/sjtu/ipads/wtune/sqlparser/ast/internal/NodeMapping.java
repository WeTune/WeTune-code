package sjtu.ipads.wtune.sqlparser.ast.internal;

import sjtu.ipads.wtune.sqlparser.ast.SQLNode;

public interface NodeMapping {
  boolean containsReferee(SQLNode node);

  boolean containsReferred(SQLNode node);

  SQLNode getRef(SQLNode node);

  SQLNode getInvertedRef(SQLNode node);

  void setRef(SQLNode n0, SQLNode n1);

  void setParent(SQLNode n0, SQLNode n1);

  SQLNode getParent(SQLNode child);

  static NodeMapping empty() {
    return NodeMappingImpl.build();
  }

  static NodeMapping basedOn(NodeMapping base) {
    return NodeMappingImpl.build(base);
  }
}
