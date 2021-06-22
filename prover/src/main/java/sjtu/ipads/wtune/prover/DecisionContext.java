package sjtu.ipads.wtune.prover;

import java.util.Collection;
import sjtu.ipads.wtune.prover.expr.UExpr;
import sjtu.ipads.wtune.sqlparser.schema.Column;
import sjtu.ipads.wtune.sqlparser.schema.Constraint;
import sjtu.ipads.wtune.sqlparser.schema.Schema;
import sjtu.ipads.wtune.sqlparser.schema.Table;

public interface DecisionContext extends ProofContext {
  void setSchema(Schema schema);

  Collection<Table> usedTables();

  Collection<Column> usedColumns();

  Collection<Constraint> uniqueKeys();

  Collection<Constraint> foreignKeys();

  static DecisionContext make(UExpr expr0, UExpr expr1) {
    return new DecisionContextImpl(expr0, expr1);
  }
}
