package sjtu.ipads.wtune.prover;

import static java.util.Objects.requireNonNull;
import static sjtu.ipads.wtune.prover.expr.UExpr.suffixTraversal;
import static sjtu.ipads.wtune.sqlparser.ast.constants.ConstraintType.PRIMARY;
import static sjtu.ipads.wtune.sqlparser.ast.constants.ConstraintType.UNIQUE;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
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
import sjtu.ipads.wtune.sqlparser.ast.constants.ConstraintType;
import sjtu.ipads.wtune.sqlparser.schema.Column;
import sjtu.ipads.wtune.sqlparser.schema.Constraint;
import sjtu.ipads.wtune.sqlparser.schema.Schema;
import sjtu.ipads.wtune.sqlparser.schema.Table;

public class DecisionContextImpl implements DecisionContext {
  private final UExpr expr0, expr1;
  private final List<Proof> proofStack;

  private Schema schema;
  private Collection<Table> usedTables;
  private Collection<Column> usedColumns;
  private Collection<Constraint> uniqueKeys, foreignKeys;
  private int nextProofId = 0;

  DecisionContextImpl(UExpr expr0, UExpr expr1) {
    this.expr0 = requireNonNull(expr0);
    this.expr1 = requireNonNull(expr1);
    this.proofStack = new ArrayList<>();
  }

  @Override
  public void setSchema(Schema schema) {
    this.schema = requireNonNull(schema);
  }

  @Override
  public Proof newProof() {
    final Proof proof = Proof.makeNull().setName("lemma_" + (nextProofId++));
    proofStack.add(proof);
    return proof;
  }

  @Override
  public Proof getProof(int idx) {
    if (idx >= 0) return proofStack.get(idx);
    else return proofStack.get(proofStack.size() - idx);
  }

  @Override
  public Proof getProof(String name) {
    if (name == null) return null;

    for (Proof proof : proofStack)
      if (name.equals(proof.name())) {
        return proof;
      }

    return null;
  }

  @Override
  public Collection<Table> usedTables() {
    if (schema == null) return Collections.emptySet();
    if (usedTables != null) return usedTables;

    usedTables = new HashSet<>();
    usedTables.addAll(collectUsedTables(expr0));
    usedTables.addAll(collectUsedTables(expr1));
    return usedTables;
  }

  @Override
  public Collection<Column> usedColumns() {
    if (schema == null) return Collections.emptySet();
    if (usedColumns != null) return usedColumns;

    usedColumns = new HashSet<>();
    usedColumns.addAll(collectUsedColumns(expr0));
    usedColumns.addAll(collectUsedColumns(expr1));
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

  private Collection<Table> collectUsedTables(UExpr expr) {
    final Schema schema = this.schema;
    return suffixTraversal(expr).stream()
        .filter(it -> it.kind() == Kind.TABLE)
        .map(it -> (TableTerm) it)
        .map(TableTerm::name)
        .map(Object::toString)
        .map(schema::table)
        .collect(Collectors.toSet());
  }

  private Collection<Column> collectUsedColumns(UExpr expr) {
    final Schema schema = this.schema;
    final List<UExpr> terms = suffixTraversal(expr);

    final Map<Tuple, Table> provence =
        terms.stream()
            .filter(it -> it.kind() == Kind.TABLE)
            .map(it -> (TableTerm) it)
            .collect(Collectors.toMap(TableTerm::tuple, it -> schema.table(it.name().toString())));

    final List<Tuple> projTuples = new ArrayList<>();
    for (UExpr term : terms) collectProjTuples(term, projTuples);

    final List<Column> usedColumns = new ArrayList<>(provence.size());
    for (Tuple tuple : projTuples) {
      final Table table = provence.get(tuple.base()[0]);
      if (table == null)
        throw new IllegalArgumentException("unknown table " + tuple.base()[0].name());

      final Column column = table.column(tuple.name().toString());
      if (column == null)
        throw new IllegalArgumentException(
            "unknown column %s.%s".formatted(table.name(), tuple.name()));

      usedColumns.add(column);
    }

    return usedColumns;
  }

  private void collectProjTuples(UExpr term, List<Tuple> dest) {
    if (term.kind() == Kind.EQ_PRED) {
      final EqPredTerm eqPred = (EqPredTerm) term;
      collectProjTuples(eqPred.left(), dest);
      collectProjTuples(eqPred.right(), dest);

    } else if (term.kind() == Kind.PRED) {
      final UninterpretedPredTerm predTerm = (UninterpretedPredTerm) term;
      for (Tuple tuple : predTerm.tuple()) collectProjTuples(tuple, dest);
    }
  }

  private void collectProjTuples(Tuple tuple, List<Tuple> dest) {
    if (tuple.isBase() || tuple.isConstant()) return;
    if (tuple.isProjected()) {
      final Tuple base = tuple.base()[0];
      if (!base.isBase()) dest.add(tuple);
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
