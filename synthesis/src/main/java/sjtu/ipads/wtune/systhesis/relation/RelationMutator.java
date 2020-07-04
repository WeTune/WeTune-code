package sjtu.ipads.wtune.systhesis.relation;

import sjtu.ipads.wtune.sqlparser.SQLNode;
import sjtu.ipads.wtune.stmt.attrs.Relation;
import sjtu.ipads.wtune.stmt.statement.Statement;

interface RelationMutator {
  boolean isValid(SQLNode node);

  Relation target();

  void modifyGraph();

  void undoModifyGraph();

  SQLNode modifyAST(Statement stmt, SQLNode root);
}
