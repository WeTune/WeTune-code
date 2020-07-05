package sjtu.ipads.wtune.systhesis.relation;

import sjtu.ipads.wtune.sqlparser.SQLNode;
import sjtu.ipads.wtune.stmt.attrs.Relation;
import sjtu.ipads.wtune.stmt.statement.Statement;

interface RelationMutator {
  Relation target();

  boolean isValid(SQLNode root);

  void modifyGraph(SQLNode root);

  void undoModifyGraph();

  SQLNode modifyAST(Statement stmt, SQLNode root);
}
