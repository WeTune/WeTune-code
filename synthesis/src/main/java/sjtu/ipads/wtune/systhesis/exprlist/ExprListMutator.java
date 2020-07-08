package sjtu.ipads.wtune.systhesis.exprlist;

import sjtu.ipads.wtune.sqlparser.SQLNode;

public interface ExprListMutator {
  SQLNode target();

  SQLNode modifyAST(SQLNode root);
}
