package sjtu.ipads.wtune.sqlparser.plan;

import sjtu.ipads.wtune.sqlparser.ast.ASTNode;
import sjtu.ipads.wtune.sqlparser.ast.constants.ExprKind;
import sjtu.ipads.wtune.sqlparser.ast.constants.LiteralType;

import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;

import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;
import static sjtu.ipads.wtune.common.utils.Commons.isEmpty;
import static sjtu.ipads.wtune.common.utils.FuncUtils.listMap;
import static sjtu.ipads.wtune.sqlparser.ast.ASTNode.*;
import static sjtu.ipads.wtune.sqlparser.ast.ExprFields.*;
import static sjtu.ipads.wtune.sqlparser.ast.NodeFields.*;
import static sjtu.ipads.wtune.sqlparser.ast.TableSourceFields.*;
import static sjtu.ipads.wtune.sqlparser.ast.constants.BinaryOp.*;
import static sjtu.ipads.wtune.sqlparser.ast.constants.ExprKind.*;
import static sjtu.ipads.wtune.sqlparser.ast.constants.JoinType.INNER_JOIN;
import static sjtu.ipads.wtune.sqlparser.ast.constants.JoinType.LEFT_JOIN;
import static sjtu.ipads.wtune.sqlparser.ast.constants.NodeType.*;
import static sjtu.ipads.wtune.sqlparser.ast.constants.SetOperationOption.ALL;
import static sjtu.ipads.wtune.sqlparser.ast.constants.SetOperationOption.DISTINCT;
import static sjtu.ipads.wtune.sqlparser.ast.constants.TableSourceKind.*;
import static sjtu.ipads.wtune.sqlparser.plan.PlanSupport.isDependentRef;
import static sjtu.ipads.wtune.sqlparser.plan.PlanSupport.isWildcardProj;

class AstTranslator {
  private final PlanNode plan;
  private final PlanContext ctx;
  private final Deque<Query> stack;
  private final List<Ref> dependentRefs;

  private AstTranslator(PlanNode plan, PlanContext ctx, boolean dependentAsPlaceholder) {
    this.plan = requireNonNull(plan);
    this.ctx = requireNonNull(ctx);
    this.stack = new LinkedList<>();
    this.dependentRefs = dependentAsPlaceholder ? new ArrayList<>() : null;
  }

  static Expr translate(PlanNode plan, boolean dependentAsPlaceholder) {
    final AstTranslator builder = new AstTranslator(plan, plan.context(), dependentAsPlaceholder);
    builder.onNode(plan);

    assert !builder.stack.isEmpty();

    final ASTNode ast = builder.stack.peek().assembleAsQuery();
    return new ExprImpl(builder.dependentRefs == null ? RefBag.empty() : RefBag.mk(builder.dependentRefs), ast);
  }

  static ASTNode translate(PlanNode plan) {
    return translate(plan, false).template();
  }

  private void onNode(PlanNode node) {
    for (PlanNode predecessor : node.predecessors()) onNode(predecessor);

    switch (node.kind()) {
      case INPUT -> onInput((InputNode) node);
      case INNER_JOIN, LEFT_JOIN -> onJoin((JoinNode) node);
      case SIMPLE_FILTER -> onPlainFilter((SimpleFilterNode) node);
      case IN_SUB_FILTER -> onInSubFilter((InSubFilterNode) node);
      case EXISTS_FILTER -> onExistsFilter((ExistsFilterNode) node);
      case PROJ -> onProj((ProjNode) node);
      case AGG -> onAgg((AggNode) node);
      case SORT -> onSort((SortNode) node);
      case LIMIT -> onLimit((LimitNode) node);
      case UNION -> onUnion((SetOpNode) node);
      default -> throw failed("unsupported operator " + node.kind());
    }
  }

  private void onInput(InputNode node) {
    final ASTNode tableName = node(TABLE_NAME);
    tableName.set(TABLE_NAME_TABLE, node.table().name());

    final ASTNode table = tableSource(SIMPLE_SOURCE);
    table.set(SIMPLE_TABLE, tableName);
    table.set(SIMPLE_ALIAS, node.values().qualification());

    stack.push(Query.from(table));
  }

  private void onJoin(JoinNode node) {
    final ASTNode rhs = stack.pop().assembleAsSource();
    final ASTNode lhs = stack.pop().assembleAsSource();

    final ASTNode join = tableSource(JOINED_SOURCE);
    join.set(JOINED_LEFT, lhs);
    join.set(JOINED_RIGHT, rhs);
    join.set(JOINED_ON, interpolate0(node.condition()));
    join.set(JOINED_TYPE, node.kind() == OperatorType.INNER_JOIN ? INNER_JOIN : LEFT_JOIN);

    stack.push(Query.from(join));
  }

  private void onPlainFilter(SimpleFilterNode node) {
    assert !stack.isEmpty();
    final Query q = stack.peek();
    final ASTNode pred = interpolate0(node.predicate());
    q.appendFilter(pred, true);
  }

  private void onInSubFilter(InSubFilterNode node) {
    final ASTNode subquery = stack.pop().assembleAsQuery();
    final ASTNode lhs = interpolate0(node.lhsExpr());
    final ASTNode queryExpr = expr(QUERY_EXPR);
    queryExpr.set(QUERY_EXPR_QUERY, subquery);

    final ASTNode inSub = expr(BINARY);
    inSub.set(BINARY_OP, IN_SUBQUERY);
    inSub.set(BINARY_LEFT, lhs);
    inSub.set(BINARY_RIGHT, queryExpr);

    assert !stack.isEmpty();
    stack.peek().appendFilter(inSub, true);
  }

  private void onExistsFilter(ExistsFilterNode node) {
    final ASTNode subquery = stack.pop().assembleAsQuery();
    final ASTNode queryExpr = expr(QUERY_EXPR);
    queryExpr.set(QUERY_EXPR_QUERY, subquery);

    final ASTNode expr = expr(EXISTS);
    expr.set(EXISTS_SUBQUERY_EXPR, queryExpr);

    assert !stack.isEmpty();
    stack.peek().appendFilter(expr, true);
  }

  private void onProj(ProjNode node) {
    assert !stack.isEmpty();
    final Query q = stack.peek();
    if (node.successor() == null && isWildcardProj(node))
      q.setProjection(singletonList(makeWildcard(null)));
    else q.setProjection(listMap(node.values(), this::toSelectItem));

    q.setQualification(node.values().qualification());
    q.setForcedDistinct(node.isDeduplicated());
  }

  private void onAgg(AggNode node) {
    assert !stack.isEmpty();
    final Query q = stack.peek();
    q.setQualification(node.values().qualification());
    q.setGroupKeys(listMap(node.groups(), this::interpolate0));
    q.setAggregation(listMap(node.values(), this::toSelectItem));
    q.setHaving(interpolate0(node.having()));
  }

  private void onSort(SortNode node) {
    assert !stack.isEmpty();
    stack.peek().setOrderKeys(listMap(node.orders(), this::interpolate0));
  }

  private void onLimit(LimitNode node) {
    assert !stack.isEmpty();
    final Query q = stack.peek();
    q.setLimit(interpolate0(node.limit()));
    q.setOffset(interpolate0(node.limit()));
  }

  private void onUnion(SetOpNode node) {
    final ASTNode rhs = stack.pop().assembleAsQuery();
    final ASTNode lhs = stack.pop().assembleAsQuery();

    final ASTNode setOp = node(SET_OP);
    setOp.set(SET_OP_LEFT, lhs);
    setOp.set(SET_OP_RIGHT, rhs);
    setOp.set(SET_OP_TYPE, node.operation());
    setOp.set(SET_OP_OPTION, node.distinct() ? DISTINCT : ALL);

    stack.push(Query.from(makeDerived(setOp, node.values().qualification())));
  }

  private ASTNode interpolate0(Expr expr) {
    if (expr == null) return null;

    final RefBag refs = expr.refs();
    final List<Value> values = ctx.deRef(refs);

    if (dependentRefs != null)
      for (int i = 0, bound = refs.size(); i < bound; i++) {
        final Ref ref = refs.get(i);
        if (isDependentRef(ref, ctx)) {
          values.set(i, null);
          dependentRefs.add(ref);
        }
      }

    return expr.interpolate(values);
  }

  private RuntimeException failed(String reason) {
    throw new IllegalArgumentException("failed to build AST. [" + reason + "] " + plan);
  }

  private ASTNode toSelectItem(Value v) {
    if (v instanceof ExprValue) return interpolate0(v.expr());
    if (v instanceof WildcardValue) return makeWildcard(v.qualification());
    throw new IllegalArgumentException();
  }

  private static ASTNode makeWildcard(String qualification) {
    final ASTNode wildcard = expr(WILDCARD);

    if (qualification != null) {
      final ASTNode tableName = node(TABLE_NAME);
      tableName.set(TABLE_NAME_TABLE, qualification);
      wildcard.set(WILDCARD_TABLE, tableName);
    }

    final ASTNode item = node(SELECT_ITEM);
    item.set(SELECT_ITEM_EXPR, wildcard);

    return item;
  }

  private static ASTNode makeDerived(ASTNode query, String qualification) {
    final ASTNode source = tableSource(DERIVED_SOURCE);
    source.set(DERIVED_SUBQUERY, query);
    source.set(DERIVED_ALIAS, qualification);
    return source;
  }

  private static class Query {
    private List<ASTNode> projection;
    private ASTNode filter;
    private ASTNode source;
    private List<ASTNode> groupKeys;
    private List<ASTNode> orderKeys;
    private ASTNode having;
    private ASTNode limit;
    private ASTNode offset;
    private String qualification;
    private boolean forcedDistinct;

    private static Query from(ASTNode source) {
      if (!TABLE_SOURCE.isInstance(source)) throw new IllegalArgumentException();
      final Query rel = new Query();
      rel.source = source;
      return rel;
    }

    public void appendFilter(ASTNode filterNode, boolean conjunctive) {
      if (projection == null)
        if (filter == null) filter = filterNode;
        else {
          final ASTNode newFilter = expr(ExprKind.BINARY);
          newFilter.set(BINARY_LEFT, filter);
          newFilter.set(BINARY_RIGHT, filterNode);
          newFilter.set(BINARY_OP, conjunctive ? AND : OR);
          filter = newFilter;
        }
      else {
        source = this.assembleAsSource();
        projection = null;
        groupKeys = null;
        having = null;
        orderKeys = null;
        limit = null;
        offset = null;
        qualification = null;
        forcedDistinct = false;
        filter = filterNode;
      }
    }

    public void setProjection(List<ASTNode> projection) {
      if (this.projection == null) this.projection = projection;
      else {
        this.source = this.assembleAsSource();
        this.projection = projection;
        this.filter = null;
        this.orderKeys = null;
        this.offset = null;
        this.limit = null;
      }

      if (projection.isEmpty()) {
        final ASTNode expr = expr(LITERAL);
        expr.set(LITERAL_TYPE, LiteralType.INTEGER);
        expr.set(LITERAL_VALUE, 1);
        final ASTNode item = node(SELECT_ITEM);
        item.set(SELECT_ITEM_EXPR, expr);
        projection.add(item);
      }
    }

    public void setAggregation(List<ASTNode> aggregation) {
      this.projection = aggregation;
      this.forcedDistinct = false;
    }

    public void setGroupKeys(List<ASTNode> groupKeys) {
      assert projection != null;
      this.groupKeys = groupKeys;
    }

    public void setOrderKeys(List<ASTNode> orderKeys) {
      this.orderKeys = orderKeys;
    }

    public void setLimit(ASTNode limit) {
      this.limit = limit;
    }

    public void setForcedDistinct(boolean flag) {
      forcedDistinct = flag;
    }

    public void setOffset(ASTNode offset) {
      this.offset = offset;
    }

    public void setQualification(String qualification) {
      this.qualification = qualification;
    }

    public void setHaving(ASTNode having) {
      this.having = having;
    }

    public ASTNode assembleAsQuery() {
      if (filter == null && projection == null && DERIVED_SOURCE.isInstance(source)) {
        return source.get(DERIVED_SUBQUERY).get(QUERY_EXPR_QUERY);
      }

      final ASTNode querySpec = node(QUERY_SPEC);
      querySpec.set(QUERY_SPEC_SELECT_ITEMS, selectItems());
      querySpec.set(QUERY_SPEC_FROM, source);
      if (forcedDistinct) querySpec.set(QUERY_SPEC_DISTINCT, true);
      if (filter != null) querySpec.set(QUERY_SPEC_WHERE, filter);
      if (!isEmpty(groupKeys)) querySpec.set(QUERY_SPEC_GROUP_BY, groupKeys);
      if (having != null) querySpec.set(QUERY_SPEC_HAVING, having);

      final ASTNode query = node(QUERY);
      query.set(QUERY_BODY, querySpec);

      if (!isEmpty(orderKeys)) query.set(QUERY_ORDER_BY, orderKeys);
      if (limit != null) query.set(QUERY_LIMIT, limit);
      if (offset != null) query.set(QUERY_OFFSET, offset);

      return query;
    }

    public ASTNode assembleAsSource() {
      if (projection == null && filter == null) return source;
      return makeDerived(assembleAsQuery(), qualification);
    }

    private List<ASTNode> selectItems() {
      if (projection != null) return projection;
      else return singletonList(makeWildcard(null));
    }
  }
}
