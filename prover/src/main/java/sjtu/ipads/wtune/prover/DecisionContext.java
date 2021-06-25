package sjtu.ipads.wtune.prover;

import java.util.Collection;
import sjtu.ipads.wtune.prover.normalform.Disjunction;
import sjtu.ipads.wtune.sqlparser.schema.Column;
import sjtu.ipads.wtune.sqlparser.schema.Constraint;
import sjtu.ipads.wtune.sqlparser.schema.Schema;
import sjtu.ipads.wtune.sqlparser.schema.Table;

public interface DecisionContext extends ProofContext {

  void setExpr0(Disjunction d);

  void setExpr1(Disjunction d);

  Collection<Table> usedTables();

  Collection<Column> usedColumns();

  Collection<Constraint> uniqueKeys();

  Collection<Constraint> foreignKeys();

  static DecisionContext make(Schema schema) {
    return new DecisionContextImpl(schema);
  }

  static DecisionContext make(Schema schema, Disjunction d0, Disjunction d1) {
    final DecisionContext ctx = make(schema);
    ctx.setExpr0(d0);
    ctx.setExpr1(d1);
    return ctx;
  }
}
