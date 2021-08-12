package sjtu.ipads.wtune.prover.logic;

import sjtu.ipads.wtune.prover.normalform.Disjunction;
import sjtu.ipads.wtune.sqlparser.schema.Schema;

public interface LogicProver {
  enum Result {
    EQ,
    NEQ,
    UNKNOWN
  }

  Result prove(Disjunction expr0, Disjunction expr1);

  void close();

  static LogicProver mk(Schema schema, LogicCtx ctx) {
    return new LogicProverImpl(schema, ctx);
  }
}
