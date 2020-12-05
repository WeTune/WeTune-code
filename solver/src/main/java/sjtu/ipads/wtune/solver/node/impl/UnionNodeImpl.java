package sjtu.ipads.wtune.solver.node.impl;

import sjtu.ipads.wtune.solver.core.SymbolicColumnRef;
import sjtu.ipads.wtune.solver.node.AlgNode;
import sjtu.ipads.wtune.solver.node.UnionNode;
import sjtu.ipads.wtune.solver.sql.ColumnRef;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static com.google.common.collect.Sets.newHashSet;
import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static sjtu.ipads.wtune.common.utils.FuncUtils.listMap;

public class UnionNodeImpl extends BaseAlgNode implements UnionNode {
  // for cache
  private List<ColumnRef> outCols;
  private List<SymbolicColumnRef> projectedCols;

  private UnionNodeImpl(boolean isForcedDistinct, List<AlgNode> inputs) {
    super(isForcedDistinct, inputs);
  }

  public static UnionNode create(boolean isForcedDistinct, List<AlgNode> inputs) {
    return new UnionNodeImpl(isForcedDistinct, inputs);
  }

  @Override
  public List<ColumnRef> columns() {
    if (outCols != null) return outCols;

    final List<ColumnRef> columns = listMap(ColumnRef::copy, inputs().get(0).columns());
    columns.forEach(it -> it.setOwner(this));
    return outCols = columns;
  }

  @Override
  public List<SymbolicColumnRef> filtered() {
    if (projectedCols != null) return projectedCols;

    final List<List<SymbolicColumnRef>> inputCols = listMap(AlgNode::projected, inputs());

    final int numCols = inputCols.get(0).size();
    final List<SymbolicColumnRef> cols = new ArrayList<>(numCols);

    for (int i = 0; i < numCols; i++) {
      final int idx = i;
      final SymbolicColumnRef col =
          inputCols.stream().map(it -> it.get(idx)).reduce(this::unionColumn).orElseThrow();

      col.setColumnRef(columns().get(i));
      cols.add(col);
    }

    return projectedCols = cols;
  }

  @Override
  public Set<Set<SymbolicColumnRef>> uniqueCores() {
    return isForcedUnique() ? singleton(newHashSet(projected())) : emptySet();
  }

  @Override
  public boolean isSingletonOutput() {
    return false;
  }

  @Override
  public List<SymbolicColumnRef> orderKeys() {
    return Collections.emptyList();
  }

  private SymbolicColumnRef unionColumn(SymbolicColumnRef c0, SymbolicColumnRef c1) {
    final SymbolicColumnRef newCol = c0.copy();
    // MEMO: don't move updateVariable after updateCondition
    newCol.updateVariable((c, v) -> ctx.ite(c, v, c1.variable()));
    newCol.updateCondition(c1.condition(), ctx::or);
    newCol.updateNotNull(c1.notNull(), ctx::or);
    return newCol;
  }

  @Override
  public String toString() {
    return toString(0);
  }

  @Override
  public String toString(int indentLevel) {
    return FORMAT_BREAK_LINE ? toString0(indentLevel) : toString1(indentLevel);
  }

  private String toString0(int indentLevel) {
    final List<String> members = listMap(it -> it.toString(indentLevel + 2), inputs());
    return String.join("\n" + " ".repeat(indentLevel) + "UNION\n", members);
  }

  private String toString1(int indentLevel) {
    final List<String> members = listMap(it -> it.toString(indentLevel + 2), inputs());
    return String.join(" UNION ", members);
  }
}
