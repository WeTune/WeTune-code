package sjtu.ipads.wtune.solver.node.impl;

import sjtu.ipads.wtune.solver.core.SymbolicColumnRef;
import sjtu.ipads.wtune.solver.node.AlgNode;
import sjtu.ipads.wtune.solver.node.TableNode;
import sjtu.ipads.wtune.solver.schema.Column;
import sjtu.ipads.wtune.solver.schema.Table;
import sjtu.ipads.wtune.solver.sql.ColumnRef;

import java.util.*;

import static sjtu.ipads.wtune.common.utils.FuncUtils.listMap;
import static sjtu.ipads.wtune.common.utils.FuncUtils.setMap;

public class TableNodeImpl extends BaseAlgNode implements TableNode {
  private final Table table;

  private List<ColumnRef> outCols;
  private List<SymbolicColumnRef> outSymCols;

  // for cache
  private Set<Set<SymbolicColumnRef>> uniqueCores;

  private TableNodeImpl(Table table) {
    this.table = table;
  }

  public static TableNodeImpl create(Table table) {
    return new TableNodeImpl(table);
  }

  @Override
  public Table table() {
    return table;
  }

  @Override
  public List<AlgNode> inputs() {
    return Collections.emptyList();
  }

  @Override
  public List<ColumnRef> columns() {
    if (outCols != null) return outCols;
    return outCols = ColumnRef.from(this);
  }

  @Override
  public List<SymbolicColumnRef> filtered() {
    if (outSymCols != null) return outSymCols;

    final List<ColumnRef> cols = columns();
    final List<SymbolicColumnRef> symCols =
        listMap(SymbolicColumnRef::copy, ctx.symbolicColumnsOf(this));

    for (int i = 0; i < symCols.size(); i++) symCols.get(i).copy().setColumnRef(cols.get(i));

    return outSymCols = symCols;
  }

  @Override
  public List<SymbolicColumnRef> projected() {
    return filtered();
  }

  @Override
  public Set<Set<SymbolicColumnRef>> uniqueCores() {
    if (uniqueCores != null) return uniqueCores;

    final Set<Set<SymbolicColumnRef>> uniqueCores = new HashSet<>();
    for (Set<Column> uniqueKey : table().uniqueKeys())
      if (uniqueKey.stream().allMatch(Column::notNull))
        uniqueCores.add(setMap(this::getSymbolicColumn, uniqueKey));

    return this.uniqueCores = uniqueCores;
  }

  @Override
  public boolean isSingletonOutput() {
    return false;
  }

  @Override
  public List<SymbolicColumnRef> orderKeys() {
    return Collections.emptyList();
  }

  private SymbolicColumnRef getSymbolicColumn(Column column) {
    return projected().get(column.index());
  }

  @Override
  public String toString() {
    return table + ": " + columns().toString();
  }

  @Override
  public String toString(int indentLevel) {
    return table().name();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    TableNodeImpl other = (TableNodeImpl) o;
    if (this.namespace() == null || other.namespace() == null)
      throw new IllegalArgumentException("TableNode is comparable only if namespace is specified");

    return Objects.equals(table, other.table) && Objects.equals(namespace(), other.namespace());
  }

  @Override
  public int hashCode() {
    if (namespace() == null)
      throw new IllegalArgumentException("TableNode is hashable only if namespace is specified");

    return Objects.hash(table, namespace());
  }
}
