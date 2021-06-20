package sjtu.ipads.wtune.sqlparser.plan1;

import sjtu.ipads.wtune.sqlparser.ast.ASTNode;
import sjtu.ipads.wtune.sqlparser.ast.constants.BinaryOp;
import sjtu.ipads.wtune.sqlparser.ast.constants.ExprKind;
import sjtu.ipads.wtune.sqlparser.ast.constants.NodeType;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static java.util.Objects.requireNonNull;
import static sjtu.ipads.wtune.sqlparser.ast.ExprFields.*;
import static sjtu.ipads.wtune.sqlparser.ast.NodeFields.COLUMN_NAME_COLUMN;
import static sjtu.ipads.wtune.sqlparser.ast.NodeFields.COLUMN_NAME_TABLE;
import static sjtu.ipads.wtune.sqlparser.util.ColumnRefCollector.gatherColumnRefs;

class ExprImpl implements Expr {
  private RefBag refs;
  private final ASTNode template;

  ExprImpl(RefBag refs, ASTNode template) {
    this.refs = refs;
    this.template = template;
  }

  static Expr build(ASTNode node) {
    final ASTNode template = node.deepCopy();
    // unify all column refs and collect refs.
    // e.g., "user.salary = user.age * 100" becomes "?.? = ?.? * 100"
    // the refs are [user.salary, user.age]
    final List<ASTNode> columnRefs = gatherColumnRefs(template);
    final List<Ref> refs = new ArrayList<>(columnRefs.size());
    for (ASTNode columnRef : columnRefs) {
      final ASTNode columnName = columnRef.get(COLUMN_REF_COLUMN);
      final String intrinsicQualification = columnName.set(COLUMN_NAME_TABLE, "?");
      final String intrinsicName = columnName.set(COLUMN_NAME_COLUMN, "?");
      refs.add(new RefImpl(intrinsicQualification, intrinsicName));
    }
    return new ExprImpl(new RefBagImpl(refs), template);
  }

  static Expr buildEquiCond(RefBag lhsRefs, RefBag rhsRefs) {
    if (lhsRefs.size() != rhsRefs.size())
      throw new IllegalArgumentException(
          "unmatched #refs for equi condition. LHS=" + lhsRefs + ", RHS=" + rhsRefs);

    final List<Ref> refs = new ArrayList<>(lhsRefs.size() << 1);
    for (int i = 0, bound = lhsRefs.size(); i < bound; i++) {
      refs.add(lhsRefs.get(i));
      refs.add(rhsRefs.get(i));
    }

    ASTNode template = makePrimitiveEquiCond();
    for (int i = 0, bound = lhsRefs.size() - 1; i < bound; i++) {
      final ASTNode conjunction = ASTNode.expr(ExprKind.BINARY);
      conjunction.set(BINARY_OP, BinaryOp.AND);
      conjunction.set(BINARY_LEFT, template);
      conjunction.set(BINARY_RIGHT, makePrimitiveEquiCond());

      template = conjunction;
    }

    return new ExprImpl(new RefBagImpl(refs), template);
  }

  private static ASTNode makePrimitiveEquiCond() {
    final ASTNode lhsName = ASTNode.node(NodeType.COLUMN_NAME);
    lhsName.set(COLUMN_NAME_TABLE, "?");
    lhsName.set(COLUMN_NAME_COLUMN, "?");

    final ASTNode rhsName = ASTNode.node(NodeType.COLUMN_NAME);
    rhsName.set(COLUMN_NAME_TABLE, "?");
    rhsName.set(COLUMN_NAME_COLUMN, "?");

    final ASTNode lhsRef = ASTNode.expr(ExprKind.COLUMN_REF);
    lhsRef.set(COLUMN_REF_COLUMN, lhsName);

    final ASTNode rhsRef = ASTNode.expr(ExprKind.COLUMN_REF);
    rhsRef.set(COLUMN_REF_COLUMN, rhsName);

    final ASTNode expr = ASTNode.expr(ExprKind.BINARY);
    expr.set(BINARY_OP, BinaryOp.EQUAL);
    expr.set(BINARY_LEFT, lhsRef);
    expr.set(BINARY_RIGHT, rhsRef);

    return expr;
  }

  @Override
  public RefBag refs() {
    return refs;
  }

  @Override
  public ASTNode template() {
    return template;
  }

  @Override
  public ASTNode interpolate(ValueBag values) {
    requireNonNull(values);
    if (values.size() != refs.size())
      throw new IllegalArgumentException("mismatched values and refs");

    final ASTNode node = template.deepCopy();
    final List<ASTNode> columnRefs = gatherColumnRefs(node);
    for (int i = 0, bound = columnRefs.size(); i < bound; i++) {
      final ASTNode name = columnRefs.get(i).get(COLUMN_REF_COLUMN);
      final Value value = values.get(i);
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
