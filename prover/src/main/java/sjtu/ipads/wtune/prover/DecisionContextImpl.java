package sjtu.ipads.wtune.prover;

import static java.util.Objects.requireNonNull;
import static sjtu.ipads.wtune.sqlparser.ast.constants.ConstraintType.PRIMARY;
import static sjtu.ipads.wtune.sqlparser.ast.constants.ConstraintType.UNIQUE;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import sjtu.ipads.wtune.prover.expr.EqPredTerm;
import sjtu.ipads.wtune.prover.expr.TableTerm;
import sjtu.ipads.wtune.prover.expr.Tuple;
import sjtu.ipads.wtune.prover.expr.UExpr;
import sjtu.ipads.wtune.prover.expr.UExpr.Kind;
import sjtu.ipads.wtune.prover.expr.UninterpretedPredTerm;
import sjtu.ipads.wtune.prover.normalform.Conjunction;
import sjtu.ipads.wtune.prover.normalform.Disjunction;
import sjtu.ipads.wtune.sqlparser.ast.constants.ConstraintType;
import sjtu.ipads.wtune.sqlparser.schema.Column;
import sjtu.ipads.wtune.sqlparser.schema.Constraint;
import sjtu.ipads.wtune.sqlparser.schema.Schema;
import sjtu.ipads.wtune.sqlparser.schema.Table;

final class DecisionContextImpl extends ProofContextImpl implements DecisionContext {
  private final Schema schema;
  private Disjunction d0, d1;
  private Provence provence0, provence1;
  private Collection<Table> usedTables;
  private Collection<Column> usedColumns;
  private Collection<Constraint> uniqueKeys, foreignKeys;

  DecisionContextImpl(Schema schema) {
    this.schema = requireNonNull(schema);
  }

  @Override
  public void setExpr0(Disjunction d) {
    d0 = requireNonNull(d);
    provence0 = new Provence(schema, d);
  }

  @Override
  public void setExpr1(Disjunction d) {
    d1 = requireNonNull(d);
    provence1 = new Provence(schema, d);
  }

  @Override
  public Collection<Table> usedTables() {
    if (usedTables != null) return usedTables;

    usedTables = new HashSet<>();
    usedTables.addAll(provence0.tables());
    usedTables.addAll(provence1.tables());

    return usedTables;
  }

  @Override
  public Collection<Column> usedColumns() {
    if (schema == null) return Collections.emptySet();
    if (usedColumns != null) return usedColumns;

    usedColumns = new HashSet<>();
    usedColumns.addAll(provence0.columns());
    usedColumns.addAll(provence1.columns());
    return usedColumns;
  }

  @Override
  public Collection<Constraint> uniqueKeys() {
    return schema == null
        ? Collections.emptySet()
        : uniqueKeys != null ? uniqueKeys : (uniqueKeys = uniqueKeys0());
  }

  @Override
  public Collection<Constraint> foreignKeys() {
    return schema == null
        ? Collections.emptySet()
        : foreignKeys != null ? foreignKeys : (foreignKeys = foreignKeys0());
  }

  private Collection<Constraint> uniqueKeys0() {
    assert schema != null;

    return usedColumns().stream()
        .map(Column::constraints)
        .flatMap(Collection::stream)
        .filter(it -> it.type() == UNIQUE || it.type() == PRIMARY)
        .collect(Collectors.toList());
  }

  private Collection<Constraint> foreignKeys0() {
    assert schema != null;

    final Collection<Table> tables = usedTables();

    return tables.stream()
        .map(Table::constraints)
        .flatMap(Collection::stream)
        .filter(it -> it.type() == ConstraintType.FOREIGN)
        .filter(it -> tables.contains(it.refTable()))
        .collect(Collectors.toList());
  }

  private static class Provence {
    private final Schema schema;
    private final Disjunction disjunction;
    private Map<Tuple, Table> tables;
    private Map<Tuple, Column> columns;

    private Provence(Schema schema, Disjunction disjunction) {
      this.schema = schema;
      this.disjunction = disjunction;
    }

    private Collection<Table> tables() {
      if (tables != null) return tables.values();
      collectTables(disjunction, tables = new HashMap<>());
      return tables.values();
    }

    private Collection<Column> columns() {
      if (columns != null) return columns.values();

      tables(); // trigger assignment of tables
      assert tables != null;

      collectColumns(disjunction, columns = new HashMap<>());
      return columns.values();
    }

    private void collectTables(Disjunction d, Map<Tuple, Table> dest) {
      for (Conjunction c : d.conjunctions()) {
        for (UExpr e : c.tables()) {
          final TableTerm tableTerm = (TableTerm) e;
          final String tableName = tableTerm.name().toString();
          final Table table = schema.table(tableName);
          if (table == null) throw new IllegalArgumentException("unknown table " + tableName);
          dest.put(tableTerm.tuple(), table);
        }

        if (c.squash() != null) collectTables(c.squash(), dest);
        if (c.negation() != null) collectTables(c.negation(), dest);
      }
    }

    private void collectColumns(Disjunction d, Map<Tuple, Column> dest) {
      final List<Tuple> buffer = new ArrayList<>(4);

      for (Conjunction c : d.conjunctions()) {
        for (UExpr e : c.predicates()) {
          buffer.clear();
          collectProjTuples(e, buffer);

          for (Tuple tuple : buffer) {
            final Table table = tables.get(tuple.base()[0]);
            if (table == null)
              throw new IllegalArgumentException("unknown table: " + tuple.base()[0].name());

            final String colName = tuple.name().toString();
            final Column column = table.column(colName);
            if (column == null)
              throw new IllegalArgumentException(
                  "unknown column: %s.%s".formatted(table.name(), colName));

            dest.put(tuple, column);
          }

          if (c.squash() != null) collectColumns(c.squash(), dest);
          if (c.negation() != null) collectColumns(c.negation(), dest);
        }
      }
    }

    private static void collectProjTuples(UExpr term, List<Tuple> dest) {
      if (term.kind() == Kind.EQ_PRED) {
        final EqPredTerm eqPred = (EqPredTerm) term;
        collectProjTuples(eqPred.left(), dest);
        collectProjTuples(eqPred.right(), dest);

      } else if (term.kind() == Kind.PRED) {
        final UninterpretedPredTerm predTerm = (UninterpretedPredTerm) term;
        for (Tuple tuple : predTerm.tuple()) collectProjTuples(tuple, dest);
      }
    }

    // collect all tuples in the form "tup.table.attr" where "tup" is a base tuple and
    // its name is not "t" ("t" is a special name denotes the only free variable)
    private static void collectProjTuples(Tuple tuple, List<Tuple> dest) {
      if (tuple.isBase() || tuple.isConstant()) return;
      if (tuple.isProjected()) {
        final Tuple base = tuple.base()[0];
        if (!base.isBase()) collectProjTuples(base.base()[0], dest);
        else if (!base.name().toString().equals("t")) dest.add(tuple);
        return;
      }
      if (tuple.isFunc()) {
        for (Tuple base : tuple.base()) collectProjTuples(base, dest);
        return;
      }

      throw new IllegalArgumentException("unexpected tuple " + tuple);
    }
  }
}
