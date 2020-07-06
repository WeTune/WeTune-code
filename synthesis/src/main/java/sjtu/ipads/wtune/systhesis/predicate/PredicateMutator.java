package sjtu.ipads.wtune.systhesis.predicate;

import sjtu.ipads.wtune.sqlparser.SQLNode;

public interface PredicateMutator {
  SQLNode target();

  SQLNode reference();

  SQLNode modifyAST(SQLNode root);

  boolean isValid(SQLNode root);
}
