package sjtu.ipads.wtune.solver.node.impl;

import sjtu.ipads.wtune.solver.core.SymbolicColumnRef;
import sjtu.ipads.wtune.solver.node.AlgNode;
import sjtu.ipads.wtune.solver.node.SPJNode;
import sjtu.ipads.wtune.solver.node.TableNode;
import sjtu.ipads.wtune.solver.schema.Table;
import sjtu.ipads.wtune.solver.sql.ColumnRef;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static sjtu.ipads.wtune.common.utils.FuncUtils.listMap;

public class TableNodeImpl extends BaseAlgNode implements TableNode {
  private final Table table;

  private List<ColumnRef> outCols;
  private List<SymbolicColumnRef> outSymCols;

  private TableNodeImpl(Table table) {
    this.table = table;
  }

  public static TableNodeImpl create(Table table) {
    return new TableNodeImpl(table);
  }

  @Override
  public TableNode setParent(AlgNode parent) {
    if (!(parent instanceof SPJNode))
      throw new IllegalArgumentException("parent of TableNode must be SPJNode");

    super.setParent(parent);
    return this;
  }

  @Override
  public SPJNode parent() {
    return (SPJNode) super.parent();
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
        listMap(SymbolicColumnRef::copy, ctx.tupleSourceOf(this));

    for (int i = 0; i < symCols.size(); i++) symCols.get(i).copy().setColumnRef(cols.get(i));

    return outSymCols = symCols;
  }

  @Override
  public List<SymbolicColumnRef> projected() {
    return filtered();
  }

  @Override
  public Table table() {
    return table;
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
    TableNodeImpl tableNode = (TableNodeImpl) o;
    return Objects.equals(table, tableNode.table);
  }

  @Override
  public int hashCode() {
    return Objects.hash(table);
  }
}
