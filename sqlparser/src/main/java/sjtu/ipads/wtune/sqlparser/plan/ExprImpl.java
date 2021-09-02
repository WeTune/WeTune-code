package sjtu.ipads.wtune.sqlparser.plan;

import sjtu.ipads.wtune.common.utils.IgnorableException;
import sjtu.ipads.wtune.sqlparser.ast.ASTNode;
import sjtu.ipads.wtune.sqlparser.ast.constants.BinaryOp;
import sjtu.ipads.wtune.sqlparser.ast.constants.ExprKind;
import sjtu.ipads.wtune.sqlparser.ast.constants.NodeType;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;
import static sjtu.ipads.wtune.common.utils.FuncUtils.listMap;
import static sjtu.ipads.wtune.sqlparser.ast.ExprFields.*;
import static sjtu.ipads.wtune.sqlparser.ast.NodeFields.COLUMN_NAME_COLUMN;
import static sjtu.ipads.wtune.sqlparser.ast.NodeFields.COLUMN_NAME_TABLE;
import static sjtu.ipads.wtune.sqlparser.util.ColumnRefCollector.gatherColumnRefs;

class ExprImpl implements Expr {
  private RefBag refs;
  private final ASTNode template;
  private List<ASTNode> holes;

  ExprImpl(RefBag refs, ASTNode template) {
    this.refs = refs;
    this.template = template;
  }

  static Expr mk(ASTNode node) {
    final ASTNode template = node.deepCopy();
    // unify all column refs and collect refs.
    // e.g., "user.salary = user.age * 100" becomes "?.? = ?.? * 100"
    // the refs are [user.salary, user.age]
    final List<ASTNode> columnRefs = gatherColumnRefs(template);
    final List<Ref> refs = new ArrayList<>(columnRefs.size());
    for (ASTNode columnRef : columnRefs) {
      final ASTNode columnName = columnRef.get(COLUMN_REF_COLUMN);
      final String intrinsicQualification = columnName.set(COLUMN_NAME_TABLE, null);
      final String intrinsicName = columnName.set(COLUMN_NAME_COLUMN, "?");
      refs.add(new RefImpl(intrinsicQualification, intrinsicName));
    }
    return new ExprImpl(RefBag.mk(refs), template);
  }

  static Expr mk(RefBag refs) {
    if (refs.isEmpty()) throw new IllegalArgumentException();
    if (refs.size() == 1) return new ExprImpl(refs, mkAnonymousColumnRef());
    else {
      final ASTNode tuple = ASTNode.expr(ExprKind.TUPLE);
      final List<ASTNode> components = listMap(refs, it -> mkAnonymousColumnRef());
      tuple.set(TUPLE_EXPRS, components);

      return new ExprImpl(refs, tuple);
    }
  }

  static Expr mkColumnRef(String qualification, String name) {
    return new ExprImpl(
        RefBag.mk(singletonList(new RefImpl(qualification, name))), mkAnonymousColumnRef());
  }

  static Expr mkEquiCond(List<Ref> lhsRefs, List<Ref> rhsRefs) {
    if (lhsRefs.size() != rhsRefs.size())
      throw new IgnorableException(
          "unmatched #refs for equi condition. LHS=" + lhsRefs + ", RHS=" + rhsRefs, true);

    final List<Ref> refs = new ArrayList<>(lhsRefs.size() << 1);
    for (int i = 0, bound = lhsRefs.size(); i < bound; i++) {
      refs.add(lhsRefs.get(i));
      refs.add(rhsRefs.get(i));
    }

    ASTNode template = mkPrimitiveEquiCond();
    for (int i = 0, bound = lhsRefs.size() - 1; i < bound; i++) {
      final ASTNode conjunction = ASTNode.expr(ExprKind.BINARY);
      conjunction.set(BINARY_OP, BinaryOp.AND);
      conjunction.set(BINARY_LEFT, template);
      conjunction.set(BINARY_RIGHT, mkPrimitiveEquiCond());

      template = conjunction;
    }

    return new ExprImpl(RefBag.mk(refs), template);
  }

  private static ASTNode mkPrimitiveEquiCond() {
    final ASTNode expr = ASTNode.expr(ExprKind.BINARY);
    expr.set(BINARY_OP, BinaryOp.EQUAL);
    expr.set(BINARY_LEFT, mkAnonymousColumnRef());
    expr.set(BINARY_RIGHT, mkAnonymousColumnRef());

    return expr;
  }

  private static ASTNode mkAnonymousColumnRef() {
    final ASTNode colName = ASTNode.node(NodeType.COLUMN_NAME);
    colName.set(COLUMN_NAME_TABLE, "?");
    colName.set(COLUMN_NAME_COLUMN, "?");

    final ASTNode colRef = ASTNode.expr(ExprKind.COLUMN_REF);
    colRef.set(COLUMN_REF_COLUMN, colName);

    return colRef;
  }

  @Override
  public RefBag refs() {
    return refs;
  }

  @Override
  public List<ASTNode> holes() {
    if (holes == null) holes = gatherColumnRefs(template);
    return holes;
  }

  @Override
  public ASTNode template() {
    return template;
  }

  @Override
  public ASTNode interpolate(List<Value> values) {
    requireNonNull(values);
    if (values.size() != refs.size())
      throw new IllegalArgumentException("mismatched values and refs");

    final ASTNode node = template.deepCopy();
    final List<ASTNode> columnRefs = gatherColumnRefs(node);
    for (int i = 0, bound = columnRefs.size(); i < bound; i++) {
      final Value value = values.get(i);
      if (value == null) continue;

      final ASTNode name = columnRefs.get(i).get(COLUMN_REF_COLUMN);
      name.set(COLUMN_NAME_TABLE, value.qualification());
      name.set(COLUMN_NAME_COLUMN, value.name());
    }
    return node;
  }

  @Override
  public void setRefs(RefBag refs) {
    if (refs.size() != this.refs.size())
      throw new IllegalArgumentException(
          "unmatched #refs in Expr[" + template + "]. refs: " + refs);
    this.refs = refs;
  }

  @Override
  public Expr copy() {
    return new ExprImpl(RefBag.mk(refs), template);
  }

  @Override
  public String toString() {
    return template.toString();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    final Expr expr = (Expr) o;
    return template.equals(expr.template());
  }

  @Override
  public int hashCode() {
    return Objects.hash(template);
  }
}
