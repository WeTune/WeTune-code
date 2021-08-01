package sjtu.ipads.wtune.prover.logic;

import sjtu.ipads.wtune.prover.normalform.Disjunction;
import sjtu.ipads.wtune.prover.uexpr.Name;
import sjtu.ipads.wtune.sqlparser.schema.Constraint;
import sjtu.ipads.wtune.sqlparser.schema.Schema;
import sjtu.ipads.wtune.sqlparser.schema.Table;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static sjtu.ipads.wtune.common.utils.FuncUtils.listMap;
import static sjtu.ipads.wtune.prover.logic.LogicSolver.Result.UNSAT;
import static sjtu.ipads.wtune.sqlparser.ast.constants.ConstraintType.FOREIGN;
import static sjtu.ipads.wtune.sqlparser.ast.constants.ConstraintType.NOT_NULL;

class LogicProverImpl implements LogicProver {
  private final Schema schema;
  private final LogicCtx ctx;
  private final LogicSolver solver;

  LogicProverImpl(Schema schema, LogicCtx ctx) {
    this.schema = schema;
    this.ctx = ctx;
    this.solver = ctx.mkSolver();
  }

  @Override
  public boolean prove(Disjunction expr0, Disjunction expr1) {
    final LogicTranslator translator = LogicTranslator.mk(ctx);
    // First let the translator know the tables and the used columns.
    final SymbolLookup usage0 = translator.prepare(expr0);
    final SymbolLookup usage1 = translator.prepare(expr1);
    final Value v0 = translator.translate(expr0, usage0);
    final Value v1 = translator.translate(expr1, usage1);

    final Set<Constraint> usedFks = new HashSet<>(4);
    gatherUsedForeignKeys(usage0, usedFks);
    gatherUsedForeignKeys(usage1, usedFks);

    final Set<Constraint> usedNotNull = new HashSet<>(4);
    gatherUsedNotNull(usage0, usedNotNull);
    gatherUsedNotNull(usage1, usedNotNull);

    final List<Proposition> fksAxioms =
        usedFks.isEmpty() ? emptyList() : listMap(usedFks, translator::translate);
    final List<Proposition> notNullAxioms =
        usedNotNull.isEmpty() ? emptyList() : listMap(usedNotNull, translator::translate);

    final Proposition proposition = v0.eq(v1).not();
    solver.reset();
    solver.add(translator.assertions());
    solver.add(fksAxioms);
    solver.add(notNullAxioms);
    solver.add(singletonList(proposition));

    return solver.solve() == UNSAT;
  }

  @Override
  public void close() {
    ctx.close();
  }

  private void gatherUsedForeignKeys(SymbolLookup lookup, Set<Constraint> buffer) {
    final Set<Name> tables = lookup.tables();
    for (Name tableName : tables) {
      final Table table = schema.table(tableName.toString());
      for (Constraint constraint : table.constraints(FOREIGN)) {
        if (tables.contains(Name.mk(constraint.refTable().name()))) {
          buffer.add(constraint);
        }
      }
    }
  }

  private void gatherUsedNotNull(SymbolLookup lookup, Set<Constraint> buffer) {
    final Set<Name> tables = lookup.tables();
    for (Name tableName : tables) {
      final Table table = schema.table(tableName.toString());
      for (Constraint constraint : table.constraints(NOT_NULL)) {
        buffer.add(constraint);
      }
    }
  }
}
