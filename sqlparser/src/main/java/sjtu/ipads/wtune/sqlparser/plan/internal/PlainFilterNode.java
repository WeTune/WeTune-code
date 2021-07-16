package sjtu.ipads.wtune.sqlparser.plan.internal;

import static sjtu.ipads.wtune.common.utils.FuncUtils.listMap;
import static sjtu.ipads.wtune.sqlparser.ast.ExprFields.BINARY_OP;
import static sjtu.ipads.wtune.sqlparser.ast.ExprFields.BINARY_RIGHT;
import static sjtu.ipads.wtune.sqlparser.ast.ExprFields.LITERAL_TYPE;
import static sjtu.ipads.wtune.sqlparser.ast.constants.ExprKind.LITERAL;
import static sjtu.ipads.wtune.sqlparser.ast.constants.ExprKind.PARAM_MARKER;
import static sjtu.ipads.wtune.sqlparser.ast.constants.NodeType.EXPR;
import static sjtu.ipads.wtune.sqlparser.plan.OperatorType.PlainFilter;
import static sjtu.ipads.wtune.sqlparser.plan.OperatorType.InSubFilter;
import static sjtu.ipads.wtune.sqlparser.util.ASTHelper.locateOtherSide;
import static sjtu.ipads.wtune.sqlparser.util.ColumnRefCollector.gatherColumnRefs;

import gnu.trove.list.TIntList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;
import sjtu.ipads.wtune.sqlparser.ast.ASTNode;
import sjtu.ipads.wtune.sqlparser.ast.constants.BinaryOp;
import sjtu.ipads.wtune.sqlparser.ast.constants.LiteralType;
import sjtu.ipads.wtune.sqlparser.plan.AttributeDef;
import sjtu.ipads.wtune.sqlparser.plan.AttributeDefBag;
import sjtu.ipads.wtune.sqlparser.plan.Expr;
import sjtu.ipads.wtune.sqlparser.plan.FilterNode;
import sjtu.ipads.wtune.sqlparser.plan.OperatorType;
import sjtu.ipads.wtune.sqlparser.plan.PlanNode;

public class PlainFilterNode extends PlanNodeBase implements FilterNode {
  protected Expr expr;
  protected List<AttributeDef> usedAttrs;

  protected Collection<AttributeDef> fixedValueAttrs;
  protected Collection<AttributeDef> nonNullValueAttrs;

  protected List<ASTNode> astNodes;

  protected PlainFilterNode(OperatorType type, Expr expr, List<AttributeDef> usedAttrs) {
    super(type);

    this.expr = expr;
    this.usedAttrs = usedAttrs;
  }

  public static FilterNode build(ASTNode expr) {
    if (!EXPR.isInstance(expr)) throw new IllegalArgumentException();
    return new PlainFilterNode(PlainFilter, Expr.make(expr), null);
  }

  public static FilterNode build(Expr expr, List<AttributeDef> usedAttrs) {
    return new PlainFilterNode(PlainFilter, expr, usedAttrs);
  }

  @Override
  public OperatorType type() {
    return PlainFilter;
  }

  @Override
  public Expr predicate() {
    return expr;
  }

  @Override
  public List<ASTNode> expr() {
    if (astNodes != null) return listMap(astNodes, ASTNode::deepCopy);

    final List<Object> components = predicate().components();
    final TIntList arity = predicate().arity();
    assert components.size() == arity.size();

    final List<ASTNode> astNodes = new ArrayList<>(components.size());
    final List<AttributeDef> attrs = usedAttributes();

    int cursor = 0;
    for (int i = 0; i < components.size(); i++) {
      final Object component = components.get(i);
      final int a = arity.get(i);
      if (cursor + a > attrs.size()) throw new IllegalStateException();

      final List<AttributeDef> used = attrs.subList(cursor, cursor + a);
      cursor += a;

      final ASTNode ast;
      if (component instanceof ASTNode) ast = ((ASTNode) component).deepCopy();
      else if (component instanceof FilterNode) ast = ((FilterNode) component).expr().get(0);
      else throw new IllegalStateException();

      updateColumnRefs(gatherColumnRefs(ast), used);
      astNodes.add(ast);
    }

    return this.astNodes = astNodes;
  }

  @Override
  public AttributeDefBag definedAttributes() {
    return predecessors()[0].definedAttributes();
  }

  @Override
  public List<AttributeDef> usedAttributes() {
    return usedAttrs;
  }

  @Override
  public Collection<AttributeDef> fixedValueAttributes() {
    if (usedAttrs == null)
      throw new IllegalStateException(
          "cannot call `fixedValueAttributes` before `resolveUsed` called");

    if (fixedValueAttrs != null) return fixedValueAttrs;
    else return fixedValueAttrs = findAttrThat(PlainFilterNode::isFixedValue);
  }

  @Override
  public Collection<AttributeDef> nonNullAttributes() {
    if (usedAttrs == null)
      throw new IllegalStateException(
          "cannot call `nonNullAttributes` before `resolveUsed` called");

    if (nonNullValueAttrs != null) return nonNullValueAttrs;
    else return nonNullValueAttrs = findAttrThat(PlainFilterNode::isNonNull);
  }

  private Collection<AttributeDef> findAttrThat(Predicate<ASTNode> check) {
    final List<ASTNode> refs = expr.columnRefs();
    final List<AttributeDef> attrs = usedAttributes();
    assert refs.size() == attrs.size();

    final List<AttributeDef> ret = new ArrayList<>(refs.size());
    for (int i = 0; i < refs.size(); i++) if (check.test(refs.get(i))) ret.add(attrs.get(i));

    return ret;
  }

  private static boolean isFixedValue(ASTNode colRef) {
    final ASTNode parent = colRef.parent();
    final BinaryOp op = parent.get(BINARY_OP);
    if (op != BinaryOp.EQUAL && op != BinaryOp.IS) return false;

    final ASTNode otherSide = locateOtherSide(parent, colRef);
    if (!PARAM_MARKER.isInstance(otherSide) && !LITERAL.isInstance(otherSide)) return false;

    ASTNode node = parent.parent();
    while (EXPR.isInstance(node)) {
      if (node.get(BINARY_OP) != BinaryOp.AND) return false;
      node = node.parent();
    }
    return true;
  }

  private static boolean isNonNull(ASTNode colRef) {
    final ASTNode parent = colRef.parent();
    final BinaryOp op = parent.get(BINARY_OP);
    return op != BinaryOp.IS || parent.get(BINARY_RIGHT).get(LITERAL_TYPE) != LiteralType.NULL;
  }

  @Override
  public void resolveUsed() {
    final AttributeDefBag inAttrs = predecessors()[0].definedAttributes();

    if (usedAttrs == null) usedAttrs = listMap(expr.columnRefs(), inAttrs::lookup);
    else usedAttrs = listMap(usedAttrs, inAttrs::lookup);

    fixedValueAttrs = null;
    nonNullValueAttrs = null;
    astNodes = null;
  }

  @Override
  public List<FilterNode> breakDown() {
    final List<Object> components = expr.components();
    final TIntList arity = expr.arity();
    assert components.size() == arity.size();

    final List<AttributeDef> usedAttrs = usedAttributes();
    final List<FilterNode> filters = new ArrayList<>(components.size());

    int cursor = 0;
    for (int i = 0; i < components.size(); i++) {
      final Object o = components.get(i);
      final int a = arity.get(i);
      final List<AttributeDef> attrs = new ArrayList<>(usedAttrs.subList(cursor, cursor + a));
      cursor += a;

      final FilterNode filter;
      if (o instanceof ASTNode) {
        filter = new PlainFilterNode(PlainFilter, Expr.make(o), attrs);

      } else if (o instanceof FilterNode) {
        assert ((FilterNode) o).type() == InSubFilter;
        filter = new SubqueryFilterNode(null, attrs);
        filter.setPredecessor(1, PlanNode.copyOnTree((PlanNode) o).predecessors()[1]);

      } else throw new IllegalStateException();

      filters.add(filter);
    }

    return filters;
  }

  @Override
  protected PlanNode copy0() {
    return new PlainFilterNode(PlainFilter, expr, usedAttributes());
  }

  @Override
  public String toString() {
    return "PlainFilter<%s>".formatted(expr());
  }
}
