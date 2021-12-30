package sjtu.ipads.wtune.sqlparser.plan1;

import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import sjtu.ipads.wtune.common.utils.ListSupport;
import sjtu.ipads.wtune.common.utils.NameSequence;
import sjtu.ipads.wtune.sqlparser.ast1.SqlContext;
import sjtu.ipads.wtune.sqlparser.ast1.SqlKind;
import sjtu.ipads.wtune.sqlparser.ast1.SqlNode;
import sjtu.ipads.wtune.sqlparser.ast1.SqlNodes;
import sjtu.ipads.wtune.sqlparser.ast1.constants.BinaryOpKind;
import sjtu.ipads.wtune.sqlparser.ast1.constants.JoinKind;
import sjtu.ipads.wtune.sqlparser.ast1.constants.SetOpKind;
import sjtu.ipads.wtune.sqlparser.ast1.constants.SetOpOption;
import sjtu.ipads.wtune.sqlparser.schema.Schema;
import sjtu.ipads.wtune.sqlparser.schema.Table;

import java.util.ArrayList;
import java.util.List;

import static java.util.Objects.requireNonNull;
import static sjtu.ipads.wtune.common.tree.TreeContext.NO_SUCH_NODE;
import static sjtu.ipads.wtune.common.utils.Commons.*;
import static sjtu.ipads.wtune.common.utils.IterableSupport.any;
import static sjtu.ipads.wtune.sqlparser.SqlSupport.copyAst;
import static sjtu.ipads.wtune.sqlparser.ast1.ExprFields.*;
import static sjtu.ipads.wtune.sqlparser.ast1.ExprKind.*;
import static sjtu.ipads.wtune.sqlparser.ast1.SqlKind.SelectItem;
import static sjtu.ipads.wtune.sqlparser.ast1.SqlKind.TableSource;
import static sjtu.ipads.wtune.sqlparser.ast1.SqlNodeFields.*;
import static sjtu.ipads.wtune.sqlparser.ast1.TableSourceFields.*;
import static sjtu.ipads.wtune.sqlparser.ast1.constants.BinaryOpKind.AND;
import static sjtu.ipads.wtune.sqlparser.ast1.constants.BinaryOpKind.IN_SUBQUERY;
import static sjtu.ipads.wtune.sqlparser.plan1.PlanSupport.*;
import static sjtu.ipads.wtune.sqlparser.util.ColRefGatherer.gatherColRefs;

class PlanBuilder {
  private static final int FAIL = Integer.MIN_VALUE;

  private final SqlNode ast;
  private final Schema schema;
  private final PlanContext plan;
  private final ValuesRegistry valuesReg;
  private final SqlContext tmpCtx;
  private final NameSequence synNameSeq;

  private String error;

  PlanBuilder(SqlNode ast, Schema schema) {
    this.ast = requireNonNull(ast);
    this.schema = requireNonNull(coalesce(ast.context().schema(), schema));
    this.plan = PlanContext.mk(schema);
    this.valuesReg = plan.valuesReg();
    this.tmpCtx = SqlContext.mk(8);
    this.synNameSeq = NameSequence.mkIndexed(SYN_NAME_PREFIX, 0);
  }

  boolean build() {
    try {
      return build0(ast) != FAIL;
    } catch (RuntimeException ex) {
      error = dumpException(ex);
      return false;
    }
  }

  PlanContext plan() {
    return plan;
  }

  String lastError() {
    return error;
  }

  private int build0(SqlNode ast) {
    final SqlKind kind = ast.kind();

    switch (kind) {
      case TableSource:
        return buildTableSource(ast);
      case SetOp:
        return buildSetOp(ast);
      case Query: {
        int nodeId = build0(ast.$(Query_Body));
        nodeId = buildSort(ast.$(Query_OrderBy), nodeId);
        nodeId = buildLimit(ast.$(Query_Limit), ast.$(Query_Offset), nodeId);
        return nodeId;
      }
      case QuerySpec: {
        int nodeId = buildTableSource(ast.$(QuerySpec_From));
        nodeId = buildFilters(ast.$(QuerySpec_Where), nodeId);
        nodeId = buildProjection(ast, nodeId);
        return nodeId;
      }
      default:
        return onError(FAILURE_INVALID_QUERY + ast);
    }
  }

  private int buildSetOp(SqlNode setOp) {
    final int lhs = build0(setOp.$(SetOp_Left));
    final int rhs = build0(setOp.$(SetOp_Right));

    if (lhs == FAIL || rhs == FAIL) return FAIL;

    final SetOpKind opKind = setOp.$(SetOp_Kind);
    final boolean deduplicated = setOp.$(SetOp_Option) == SetOpOption.DISTINCT;
    final SetOpNode setOpNode = SetOpNode.mk(deduplicated, opKind);

    final int nodeId = plan.bindNode(setOpNode);
    plan.setChild(nodeId, 0, lhs);
    plan.setChild(nodeId, 1, rhs);

    return nodeId;
  }

  private int buildProjection(SqlNode querySpec, int child) {
    if (child == FAIL) return FAIL;

    final SqlNodes items = querySpec.$(QuerySpec_SelectItems);
    final SqlNodes groupBys = coalesce(querySpec.$(QuerySpec_GroupBy), SqlNodes.mkEmpty());
    final SqlNode having = querySpec.$(QuerySpec_Having);
    final boolean deduplicated = querySpec.isFlag(QuerySpec_Distinct);

    final List<String> attrNames = new ArrayList<>(items.size());
    final List<Expression> attrExprs = new ArrayList<>(items.size());

    if (!containsAgg(items) && groupBys.isEmpty()) {
      mkAttrs(child, items, attrNames, attrExprs);
      final ProjNode proj = ProjNode.mk(deduplicated, attrNames, attrExprs);
      final int projNodeId = plan.bindNode(proj);
      if (child != NO_SUCH_NODE) plan.setChild(projNodeId, 0, child);

      return projNodeId;

    } else {
      /*
       We translate aggregation as Agg(Proj(..)).
       The inner Proj projects all the attributes used in aggregations.
       e.g., SELECT SUM(salary) FROM T GROUP BY dept HAVING MAX(age) > 40
          => Proj[salary]

       (Actually such statement is invalid in standard SQL,
        in which all the columns appear in GROUP BY must also appear in selection.
        This is a vendor-extension.)
      */

      if (any(items, it1 -> it1.$(SelectItem_Expr).$(Aggregate_WindowSpec) != null))
        return onError(FAILURE_UNSUPPORTED_FEATURE + "window function");

      // 1. Extract column refs used in selectItems, groups and having
      final List<SqlNode> colRefs = new ArrayList<>(items.size() + groupBys.size() + 1);
      colRefs.addAll(gatherColRefs(items));
      colRefs.addAll(gatherColRefs(groupBys));
      if (having != null) colRefs.addAll(gatherColRefs(having));

      // 2. build Proj node
      final ProjNode proj = mkForwardProj(colRefs, containsDeduplicatedAgg(items));

      // 3. build Agg node
      mkAttrs(child, items, attrNames, attrExprs);
      final List<Expression> groupByExprs = new ArrayList<>(groupBys.size());
      for (SqlNode groupBy : groupBys) groupByExprs.add(Expression.mk(groupBy));
      final Expression havingExpr = having == null ? null : Expression.mk(having);

      final AggNode agg = AggNode.mk(deduplicated, attrNames, attrExprs, groupByExprs, havingExpr);

      // 4. assemble
      final int projNodeId = plan.bindNode(proj);
      final int aggNodeId = plan.bindNode(agg);
      if (child != NO_SUCH_NODE) plan.setChild(projNodeId, 0, child);
      plan.setChild(aggNodeId, 0, projNodeId);

      return aggNodeId;
    }
  }

  private int buildFilters(SqlNode expr, int child) {
    if (child == FAIL) return FAIL;
    if (expr == null) return child;

    final TIntList filters = buildFilters0(expr, new TIntArrayList(4));
    if (filters.isEmpty()) return child;

    for (int i = 1, bound = filters.size(); i < bound; ++i)
      plan.setChild(filters.get(i), 0, filters.get(i - 1));
    if (child != NO_SUCH_NODE)
      plan.setChild(filters.get(0), 0, child);

    return filters.get(filters.size() - 1);
  }

  private TIntList buildFilters0(SqlNode expr, TIntList filters) {
    final BinaryOpKind opKind = expr.$(Binary_Op);
    if (opKind == AND) {
      buildFilters0(expr.$(Binary_Left), filters);
      buildFilters0(expr.$(Binary_Right), filters);

    } else if (opKind == IN_SUBQUERY) {
      final InSubNode filter = InSubNode.mk(Expression.mk(expr.$(Binary_Left)));
      final int subqueryId = build0(expr.$(Binary_Right).$(QueryExpr_Query));
      final int nodeId = plan.bindNode(filter);
      plan.setChild(nodeId, 1, subqueryId);

      filters.add(nodeId);

    } else if (Exists.isInstance(expr)) {
      final ExistsNode filter = ExistsNode.mk();
      final int subqueryId = build0(expr.$(Exists_Subquery).$(QueryExpr_Query));
      final int nodeId = plan.bindNode(filter);
      plan.setChild(nodeId, 1, subqueryId);

    } else if (!isBoolConstant(expr)){
      // Preclude ones like "1=1".
      final SqlNode normalized = normalizePredicate(expr, tmpCtx);
      final SimpleFilterNode filter = SimpleFilterNode.mk(Expression.mk(normalized));
      final int nodeId = plan.bindNode(filter);
      filters.add(nodeId);
    }

    return filters;
  }

  private int buildSort(SqlNodes orders, int child) {
    if (child == FAIL) return FAIL;
    if (orders == null || orders.isEmpty()) return child;

    final List<Expression> sortSpec = ListSupport.map(orders, Expression::mk);
    final SortNode sortNode = SortNode.mk(sortSpec);

    final int nodeId = plan.bindNode(sortNode);
    plan.setChild(nodeId, 0, child);

    return nodeId;
  }

  private int buildLimit(SqlNode limit, SqlNode offset, int child) {
    if (child == FAIL) return FAIL;
    if (limit == null && offset == null) return child;

    final LimitNode limitNode =
        LimitNode.mk(
            limit == null ? null : Expression.mk(limit),
            offset == null ? null : Expression.mk(limit));

    final int nodeId = plan.bindNode(limitNode);
    plan.setChild(nodeId, 0, child);

    return nodeId;
  }

  private int buildTableSource(SqlNode tableSource) {
    if (tableSource == null) return NO_SUCH_NODE;
    assert TableSource.isInstance(tableSource);
    return switch (tableSource.$(TableSource_Kind)) {
      case SimpleSource -> buildSimpleTableSource(tableSource);
      case JoinedSource -> buildJoinedTableSource(tableSource);
      case DerivedSource -> buildDerivedTableSource(tableSource);
    };
  }

  private int buildSimpleTableSource(SqlNode tableSource) {
    final String tableName = tableSource.$(Simple_Table).$(TableName_Table);
    final Table table = schema.table(tableName);

    if (table == null) return onError(FAILURE_UNKNOWN_TABLE + tableSource);

    final String alias = coalesce(tableSource.$(Simple_Alias), tableName);
    return plan.bindNode(InputNode.mk(table, alias));
  }

  private int buildJoinedTableSource(SqlNode tableSource) {
    final int lhs = build0(tableSource.$(Joined_Left));
    final int rhs = build0(tableSource.$(Joined_Right));

    final JoinKind joinKind = tableSource.$(Joined_Kind);
    final SqlNode condition = tableSource.$(Joined_On);

    final JoinNode joinNode = JoinNode.mk(joinKind, condition == null ? null : Expression.mk(condition));

    final int nodeId = plan.bindNode(joinNode);
    plan.setChild(nodeId, 0, lhs);
    plan.setChild(nodeId, 1, rhs);

    return nodeId;
  }

  private int buildDerivedTableSource(SqlNode tableSource) {
    final String alias = tableSource.$(Derived_Alias);

    if (alias == null) return onError(FAILURE_MISSING_QUALIFICATION + tableSource);

    final int subquery = build0(tableSource.$(Derived_Subquery));

    int qualifiedNodeId = subquery;
    while (!(plan.nodeAt(qualifiedNodeId) instanceof Qualified))
      qualifiedNodeId = plan.childOf(qualifiedNodeId, 0);
    ((Qualified) plan.nodeAt(qualifiedNodeId)).setQualification(alias);

    return subquery;
  }

  private int onError(String error) {
    this.error = error;
    return FAIL;
  }

  private static boolean containsDeduplicatedAgg(SqlNodes selectItem) {
    for (SqlNode item : selectItem)
      if (item.$(SelectItem_Expr).isFlag(Aggregate_Distinct)) {
        return true;
      }
    return false;
  }

  private static boolean containsAgg(SqlNodes selectItems) {
    for (SqlNode item : selectItems)
      if (Aggregate.isInstance(item.$(SelectItem_Expr))) {
        return true;
      }
    return false;
  }

  private void mkAttrs(
      int inputNodeId, SqlNodes selectItems, List<String> attrNames, List<Expression> attrExprs) {
    for (SqlNode item : selectItems) {
      final SqlNode exprAst = item.$(SelectItem_Expr);
      if (Wildcard.isInstance(exprAst)) {
        expandWildcard(inputNodeId, exprAst, attrNames, attrExprs);
      } else {
        attrNames.add(mkAttrName(item));
        attrExprs.add(Expression.mk(exprAst));
      }
    }
  }

  private void expandWildcard(
      int inputNodeId, SqlNode wildcard, List<String> attrNames, List<Expression> attrExprs) {
    final String qualification;
    if (wildcard.$(Wildcard_Table) == null) qualification = null;
    else qualification= wildcard.$(Wildcard_Table).$(TableName_Table);

    final Values values = valuesReg.valuesOf(inputNodeId);
    // A corner case is wildcard can refer to an anonymous attribute,
    // e.g., "Select * From (Select x.a + x.b From x) sub"
    // We synthesized a name for it (see mkAttrName). This leads to SQL:
    // "Select sub._anony0 From (Select x.a + x.b As _anony0 From x) sub".
    // This is okay when playing with plan, but actually the final output schema is changed.
    // We work around this issue in ToSqlTranslator.
    for (Value value : values) {
      if (qualification == null || (qualification.equals(value.qualification()))) {
        attrNames.add(value.name());
        attrExprs.add(PlanSupport.mkColRefExpr(value));
      }
    }
  }

  private String mkAttrName(SqlNode selectItem) {
    final String alias = selectItem.$(SelectItem_Alias);
    if (alias != null) return alias;

    final SqlNode exprAst = selectItem.$(SelectItem_Expr);
    if (ColRef.isInstance(exprAst)) return exprAst.$(ColRef_ColName).$(ColName_Col);

    // For some derived select-item, like 'salary / age'.
    return synNameSeq.next();
  }

  private ProjNode mkForwardProj(List<SqlNode> colRefs, boolean deduplicated) {
    final List<String> attrNames = new ArrayList<>(colRefs.size());
    final List<Expression> attrExprs = new ArrayList<>(colRefs.size());
    final NameSequence seq = NameSequence.mkIndexed("agg", 0);

    for (SqlNode colRef : colRefs) {
      final String name = seq.next();
      final SqlNode expr = copyAst(colRef, tmpCtx);
      final SqlNode selectItem = SqlNode.mk(tmpCtx, tmpCtx.mkNode(SelectItem));

      selectItem.$(SelectItem_Expr, expr);
      selectItem.$(SelectItem_Alias, seq.next());

      attrNames.add(name);
      attrExprs.add(Expression.mk(selectItem));
    }

    return ProjNode.mk(deduplicated, attrNames, attrExprs);
  }
}
