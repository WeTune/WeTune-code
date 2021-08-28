package sjtu.ipads.wtune.sqlparser.plan1;

import sjtu.ipads.wtune.sqlparser.ast.ASTNode;
import sjtu.ipads.wtune.sqlparser.ast.constants.BinaryOp;
import sjtu.ipads.wtune.sqlparser.ast.constants.ExprKind;

import java.util.ArrayList;
import java.util.List;

import static sjtu.ipads.wtune.sqlparser.ast.ExprFields.*;

class CombinedFilterNodeImpl extends SimpleFilterNodeImpl implements CombinedFilterNode {
  private final List<FilterNode> filters;

  CombinedFilterNodeImpl(List<FilterNode> filters, Expr predicate) {
    super(predicate);
    this.filters = filters;
  }

  static CombinedFilterNode mk(List<FilterNode> filters) {
    if (filters.isEmpty()) throw new IllegalArgumentException();

    final List<Ref> refs = new ArrayList<>(filters.size());
    final ASTNode fstAst = filters.get(0).predicate().template();
    ASTNode predicate = fstAst.deepCopy();
    predicate.setContext(fstAst.context());
    refs.addAll(filters.get(0).refs());

    for (FilterNode filter : filters.subList(1, filters.size())) {
      final ASTNode ast = filter.predicate().template();
      ASTNode conjunction = ASTNode.expr(ExprKind.BINARY);
      conjunction.set(BINARY_LEFT, predicate);
      conjunction.set(BINARY_RIGHT, ast.deepCopy());
      conjunction.set(BINARY_OP, BinaryOp.AND);
      conjunction.setContext(ast.context());
      predicate = conjunction;
      refs.addAll(filter.refs());
    }

    final ExprImpl expr = new ExprImpl(RefBag.mk(refs), predicate);
    final CombinedFilterNode node = new CombinedFilterNodeImpl(filters, expr);
    node.setContext(filters.get(0).context());
    return node;
  }

  @Override
  public List<FilterNode> filters() {
    return filters;
  }

  @Override
  public PlanNode copy(PlanContext ctx) {
    checkContextSet();

    final CombinedFilterNode copy = new CombinedFilterNodeImpl(filters, predicate);
    copy.setContext(ctx);

    ctx.registerRefs(copy, refs());
    for (Ref ref : refs()) ctx.setRef(ref, this.context.deRef(ref));

    return copy;
  }
}
