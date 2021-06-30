package sjtu.ipads.wtune.sqlparser.plan.internal;

import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;
import static sjtu.ipads.wtune.sqlparser.ast.constants.NodeType.EXPR;
import static sjtu.ipads.wtune.sqlparser.plan.OperatorType.InSubFilter;
import static sjtu.ipads.wtune.sqlparser.util.ColumnRefCollector.gatherColumnRefs;

import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import sjtu.ipads.wtune.sqlparser.ast.ASTNode;
import sjtu.ipads.wtune.sqlparser.plan.Expr;
import sjtu.ipads.wtune.sqlparser.plan.FilterNode;
import sjtu.ipads.wtune.sqlparser.plan.PlanNode;

public class ExprImpl implements Expr {
  private final List<Object> components;

  private List<ASTNode> columnRefs;
  private TIntList arity;

  private ExprImpl(List<Object> components, TIntList arity) {
    this.components = components;
    this.arity = arity;
  }

  public static Expr build(Object component) {
    return new ExprImpl(singletonList(component), null);
  }

  public static Expr build(List<Object> components, TIntList arity) {
    if (!components.stream().allMatch(ExprImpl::checkComponentType))
      throw new IllegalArgumentException();

    return new ExprImpl(components, requireNonNull(arity));
  }

  private static boolean checkComponentType(Object c) {
    return c instanceof ASTNode
        ? EXPR.isInstance((ASTNode) c)
        : c instanceof FilterNode && ((FilterNode) c).type() == InSubFilter;
  }

  @Override
  public List<Object> components() {
    return components;
  }

  @Override
  public List<ASTNode> columnRefs() {
    if (columnRefs == null) {
      final List<ASTNode> columnRefs = new ArrayList<>(components.size());

      for (Object component : components) {
        if (component instanceof ASTNode) {
          columnRefs.addAll(gatherColumnRefs((ASTNode) component));

        } else if (component instanceof FilterNode) {
          final FilterNode filter = (FilterNode) component;
          assert filter.type() == InSubFilter;

          final List<ASTNode> astNodes = filter.expr();
          assert astNodes.size() == 1;

          columnRefs.addAll(gatherColumnRefs(astNodes));

        } else throw new IllegalStateException();
      }

      this.columnRefs = columnRefs;
    }

    return columnRefs;
  }

  @Override
  public TIntList arity() {
    if (arity != null) return arity;
    assert components.size() == 1;
    arity = new TIntArrayList(1);
    arity.add(columnRefs().size());
    return arity;
  }

  @Override
  public boolean equals(Object other) {
    if (other == this) return true;
    if (!(other instanceof Expr)) return false;

    final List<Object> thisComponents = this.components();
    final List<Object> thatComponents = ((Expr) other).components();
    if (thisComponents.size() != thatComponents.size()) return false;

    for (int i = 0, bound = thisComponents.size(); i < bound; i++) {
      final Object thisComponent = thisComponents.get(i);
      final Object thatComponent = thatComponents.get(i);

      if (thisComponent instanceof ASTNode && thatComponent instanceof ASTNode)
        if (!Objects.equals(thisComponent, thatComponent)) return false;

      if (thisComponent instanceof FilterNode && thatComponent instanceof FilterNode)
        if (!Objects.equals(
            PlanNode.toStringOnTree((FilterNode) thisComponent),
            PlanNode.toStringOnTree((FilterNode) thatComponent))) return false;
    }

    return true;
  }

  @Override
  public int hashCode() {
    int hash = 0;
    for (Object component : components()) {
      final int componentHash;
      if (component instanceof ASTNode) componentHash = component.toString().hashCode();
      else if (component instanceof FilterNode)
        componentHash = PlanNode.toStringOnTree((FilterNode) component).hashCode();
      else throw new IllegalStateException();

      hash = hash * 31 + componentHash;
    }

    return hash;
  }

  @Override
  public String toString() {
    return components.stream()
        .map(it -> it instanceof ASTNode ? it.toString() : PlanNode.toStringOnTree((PlanNode) it))
        .collect(Collectors.joining(" AND "));
  }
}
