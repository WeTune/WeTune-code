package sjtu.ipads.wtune.sqlparser.plan.internal;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static sjtu.ipads.wtune.common.utils.FuncUtils.listMap;
import static sjtu.ipads.wtune.sqlparser.ast.ExprFields.BINARY_LEFT;
import static sjtu.ipads.wtune.sqlparser.ast.ExprFields.BINARY_OP;
import static sjtu.ipads.wtune.sqlparser.ast.ExprFields.BINARY_RIGHT;
import static sjtu.ipads.wtune.sqlparser.ast.ExprFields.QUERY_EXPR_QUERY;
import static sjtu.ipads.wtune.sqlparser.ast.ExprFields.TUPLE_EXPRS;
import static sjtu.ipads.wtune.sqlparser.ast.constants.ExprKind.BINARY;
import static sjtu.ipads.wtune.sqlparser.ast.constants.ExprKind.QUERY_EXPR;
import static sjtu.ipads.wtune.sqlparser.plan.OperatorType.SubqueryFilter;
import static sjtu.ipads.wtune.sqlparser.plan.ToASTTranslator.toAST;
import static sjtu.ipads.wtune.sqlparser.util.ColumnRefCollector.gatherColumnRefs;

import java.util.Collection;
import java.util.List;
import sjtu.ipads.wtune.sqlparser.ast.ASTNode;
import sjtu.ipads.wtune.sqlparser.ast.constants.BinaryOp;
import sjtu.ipads.wtune.sqlparser.ast.constants.ExprKind;
import sjtu.ipads.wtune.sqlparser.plan.AttributeDef;
import sjtu.ipads.wtune.sqlparser.plan.AttributeDefBag;
import sjtu.ipads.wtune.sqlparser.plan.Expr;
import sjtu.ipads.wtune.sqlparser.plan.FilterNode;
import sjtu.ipads.wtune.sqlparser.plan.OperatorType;
import sjtu.ipads.wtune.sqlparser.plan.PlanNode;

public class SubqueryFilterNode extends PlainFilterNode {
  private List<ASTNode> colRefs;

  protected SubqueryFilterNode(List<ASTNode> colRefs, List<AttributeDef> usedAttrs) {
    super(SubqueryFilter, null, usedAttrs);
    assert colRefs != null || usedAttrs != null;
    this.expr = Expr.make(this);
    this.colRefs = usedAttrs != null ? listMap(AttributeDef::makeColumnRef, usedAttrs) : colRefs;
  }

  public static FilterNode buildFromExpr(ASTNode colRefs) {
    return new SubqueryFilterNode(gatherColumnRefs(colRefs), null);
  }

  public static FilterNode buildFromAttributes(List<AttributeDef> usedAttrs) {
    return new SubqueryFilterNode(null, usedAttrs);
  }

  @Override
  public OperatorType type() {
    return SubqueryFilter;
  }

  @Override
  public List<ASTNode> expr() {
    if (astNodes != null) return singletonList(astNodes.get(0).deepCopy());

    // assemble IN-subquery expression
    final ASTNode subquery = toAST(predecessors()[1]);
    final ASTNode refExpr = wrapColumnRefs(colRefs);

    final ASTNode queryExpr = ASTNode.expr(QUERY_EXPR);
    queryExpr.set(QUERY_EXPR_QUERY, subquery);

    final ASTNode ast = ASTNode.expr(BINARY);
    ast.set(BINARY_OP, BinaryOp.IN_SUBQUERY);
    ast.set(BINARY_LEFT, refExpr);
    ast.set(BINARY_RIGHT, queryExpr);

    return this.astNodes = singletonList(ast);
  }

  @Override
  public Collection<AttributeDef> fixedValueAttributes() {
    return emptyList();
  }

  @Override
  public Collection<AttributeDef> nonNullAttributes() {
    if (usedAttrs == null)
      throw new IllegalStateException(
          "cannot call `nonNullAttributes` before `resolveUsed` called");
    return usedAttributes();
  }

  @Override
  public void resolveUsed() {
    final AttributeDefBag inAttrs = predecessors()[0].definedAttributes();

    if (usedAttrs == null) usedAttrs = listMap(inAttrs::lookup, colRefs);
    else {
      usedAttrs = listMap(inAttrs::lookup, usedAttrs);
      colRefs = listMap(AttributeDef::makeColumnRef, usedAttrs);
    }

    astNodes = null;
  }

  @Override
  public List<FilterNode> breakDown() {
    throw new UnsupportedOperationException();
  }

  @Override
  protected PlanNode copy0() {
    return new SubqueryFilterNode(colRefs, usedAttributes());
  }

  @Override
  public String toString() {
    // `colRefs` & `usedAttrs` keeps sync, so just use colRefs
    return "SubqueryFilter<%s>".formatted(colRefs);
  }

  private static ASTNode wrapColumnRefs(List<ASTNode> columnRefs) {
    if (columnRefs.isEmpty()) throw new IllegalArgumentException();
    if (columnRefs.size() == 1) return columnRefs.get(0);

    final ASTNode tuple = ASTNode.expr(ExprKind.TUPLE);
    tuple.set(TUPLE_EXPRS, columnRefs);

    return tuple;
  }
}
