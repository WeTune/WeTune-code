package sjtu.ipads.wtune.sql.plan;

import sjtu.ipads.wtune.common.utils.ListSupport;
import sjtu.ipads.wtune.sql.SqlSupport;
import sjtu.ipads.wtune.sql.ast.SqlContext;
import sjtu.ipads.wtune.sql.ast.SqlNode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static sjtu.ipads.wtune.common.tree.TreeContext.NO_SUCH_NODE;
import static sjtu.ipads.wtune.common.utils.Commons.dumpException;
import static sjtu.ipads.wtune.common.utils.IterableSupport.all;
import static sjtu.ipads.wtune.sql.SqlSupport.copyAst;
import static sjtu.ipads.wtune.sql.ast.ExprFields.*;
import static sjtu.ipads.wtune.sql.ast.ExprKind.*;
import static sjtu.ipads.wtune.sql.ast.SqlNodeFields.ColName_Col;
import static sjtu.ipads.wtune.sql.ast.SqlNodeFields.ColName_Table;
import static sjtu.ipads.wtune.sql.ast.constants.BinaryOpKind.IN_SUBQUERY;
import static sjtu.ipads.wtune.sql.plan.DependentRefInspector.inspectDepRefs;
import static sjtu.ipads.wtune.sql.plan.PlanSupport.*;

class ValueRefBinder {
  private final PlanContext plan;
  private final ValuesRegistry valuesReg;

  private String error;

  ValueRefBinder(PlanContext plan) {
    this.plan = plan;
    this.valuesReg = plan.valuesReg();
  }

  boolean bind() {
    try {
      return bind0(plan.root(), Collections.emptyList());
    } catch (RuntimeException ex) {
      error = dumpException(ex);
      return false;
    }
  }

  String lastError() {
    return error;
  }

  private boolean bind0(int nodeId, List<Value> secondaryLookup) {
    if (nodeId == NO_SUCH_NODE) return true;

    return switch (plan.kindOf(nodeId)) {
      case Limit -> bind0(plan.childOf(nodeId, 0), secondaryLookup);
      case Sort -> bind0(plan.childOf(nodeId, 0), secondaryLookup) && bindSort(nodeId);
      case SetOp -> bind0(plan.childOf(nodeId, 0), secondaryLookup)
              && bind0(plan.childOf(nodeId, 1), secondaryLookup);
      case Join -> bind0(plan.childOf(nodeId, 0), secondaryLookup)
              && bind0(plan.childOf(nodeId, 1), secondaryLookup)
              && bindJoin(nodeId);
      case Agg -> bind0(plan.childOf(nodeId, 0), secondaryLookup) && bindAgg(nodeId);
      case Proj -> bind0(plan.childOf(nodeId, 0), secondaryLookup) && bindProj(nodeId);
      case Filter -> bind0(plan.childOf(nodeId, 0), secondaryLookup)
              && bindFilter(nodeId, secondaryLookup);
      case Exists -> bind0(plan.childOf(nodeId, 0), secondaryLookup)
              && bindExists(nodeId, secondaryLookup);
      case InSub -> bind0(plan.childOf(nodeId, 0), secondaryLookup)
              && bindInSub(nodeId, secondaryLookup);
      case Input -> true;
    };
  }

  private boolean bindAgg(int nodeId) {
    // Group By & Having can use the attribute exposed by Aggregation.
    final Values primaryLookup = valuesReg.valuesOf(nodeId);
    final Values secondaryLookup = valuesReg.valuesOf(plan.childOf(plan.childOf(nodeId, 0), 0));
    final List<Value> lookup = ListSupport.join(primaryLookup, secondaryLookup);
    final AggNode aggNode = (AggNode) plan.nodeAt(nodeId);

    for (Expression attr : aggNode.attrExprs()) {
      final List<Value> refs = ListSupport.map(attr.colRefs(), it -> bindRef(it, secondaryLookup));
      if (refs.contains(null)) return false;
      valuesReg.bindValueRefs(attr, refs);
    }

    for (Expression groupBy : aggNode.groupByExprs()) {
      final List<Value> refs = ListSupport.map(groupBy.colRefs(), it -> bindRef(it, lookup));
      if (refs.contains(null)) return false;
      valuesReg.bindValueRefs(groupBy, refs);
    }

    final Expression having = aggNode.havingExpr();
    if (having != null) {
      final List<Value> refs = ListSupport.map(having.colRefs(), it -> bindRef(it, lookup));
      if (refs.contains(null)) return false;
      valuesReg.bindValueRefs(having, refs);
    }

    return true;
  }

  private boolean bindProj(int nodeId) {
    final Values lookup = valuesReg.valuesOf(plan.childOf(nodeId, 0));
    final ProjNode projNode = (ProjNode) plan.nodeAt(nodeId);

    for (Expression attr : projNode.attrExprs()) {
      final List<Value> refs = ListSupport.map(attr.colRefs(), it -> bindRef(it, lookup));
      if (refs.contains(null)) return false;
      valuesReg.bindValueRefs(attr, refs);
    }

    return true;
  }

  private boolean bindJoin(int nodeId) {
    final Values lookup = valuesReg.valuesOf(nodeId);
    final JoinNode joinNode = (JoinNode) plan.nodeAt(nodeId);
    final Expression joinCond = joinNode.joinCond();
    if (joinCond == null) return true;

    final List<Value> valueRefs = ListSupport.map(joinCond.colRefs(), it -> bindRef(it, lookup));
    valuesReg.bindValueRefs(joinCond, valueRefs);
    if (valueRefs.contains(null)) return false;

    if (!SqlSupport.isEquiJoinPredicate(joinCond.template())) return true;
    if ((valueRefs.size() & 1) == 1) return true;

    final Values lhsValues = valuesReg.valuesOf(plan.childOf(nodeId, 0));
    final List<Value> lhsRefs = new ArrayList<>(valueRefs.size() >> 1);
    final List<Value> rhsRefs = new ArrayList<>(valueRefs.size() >> 1);
    for (int i = 0, bound = valueRefs.size(); i < bound; i += 2) {
      final Value key0 = valueRefs.get(i), key1 = valueRefs.get(i + 1);
      final boolean lhs0 = lhsValues.contains(key0), lhs1 = lhsValues.contains(key1);
      if (lhs0 && !lhs1) {
        lhsRefs.add(key0);
        rhsRefs.add(key1);
      } else if (!lhs0 && lhs1) {
        lhsRefs.add(key1);
        rhsRefs.add(key0);
      } else {
        return true;
      }
    }

    plan.infoCache().putJoinKeyOf(nodeId, lhsRefs, rhsRefs);
    return true;
  }

  private boolean bindFilter(int nodeId, List<Value> secondaryLookup) {
    final Values primaryLookup = valuesReg.valuesOf(plan.childOf(nodeId, 0));
    final List<Value> lookup = ListSupport.join(primaryLookup, secondaryLookup);
    final Expression predicate = ((SimpleFilterNode) plan.nodeAt(nodeId)).predicate();
    final List<Value> valueRefs = ListSupport.map(predicate.colRefs(), it -> bindRef(it, lookup));
    valuesReg.bindValueRefs(predicate, valueRefs);
    return !valueRefs.contains(null);
  }

  private boolean bindExists(int nodeId, List<Value> secondaryLookup) {
    final Values primaryLookup = valuesReg.valuesOf(plan.childOf(nodeId, 0));
    final List<Value> lookup = ListSupport.join(primaryLookup, secondaryLookup);
    if (!bind0(plan.childOf(nodeId, 1), lookup)) return false;

    final SqlNode existsExprAst = mkExistsExpr(nodeId);
    if (existsExprAst == null) return onError(FAILURE_BAD_SUBQUERY_EXPR);

    final var deps = inspectDepRefs(plan, plan.childOf(nodeId, 1));
    final List<Value> depValueRefs = deps.getLeft();
    final List<SqlNode> depColRefs = deps.getRight();
    final Expression existsExpr = Expression.mk(existsExprAst, depColRefs);
    plan.infoCache().putSubqueryExprOf(nodeId, existsExpr);
    valuesReg.bindValueRefs(existsExpr, depValueRefs);

    return true;
  }

  private boolean bindInSub(int nodeId, List<Value> secondaryLookup) {
    final Values primaryLookup = valuesReg.valuesOf(plan.childOf(nodeId, 0));
    final List<Value> lookup = ListSupport.join(primaryLookup, secondaryLookup);
    final InSubNode inSub = (InSubNode) plan.nodeAt(nodeId);
    final Expression lhsExpr = inSub.expr();
    final List<Value> valueRefs = ListSupport.map(lhsExpr.colRefs(), it -> bindRef(it, lookup));

    valuesReg.bindValueRefs(lhsExpr, valueRefs);
    if (valueRefs.contains(null) || !bind0(plan.childOf(nodeId, 1), lookup)) return false;

    inSub.setPlain(isPlainInSub(inSub));

    /* Make expression for subquery */
    // e.g., The expr of "InSub<q0.a>(T, Proj<R.c>(Filter<p, T.b>(R)))" is
    //       "#.# IN (Select R.c From T Where p(#.#))"
    final SqlNode inSubExprAst = mkInSubExpr(nodeId);
    if (inSubExprAst == null) return onError(FAILURE_BAD_SUBQUERY_EXPR + inSub);

    // collect the dependent refs from the subquery.
    final var deps = inspectDepRefs(plan, plan.childOf(nodeId, 1));
    final List<Value> depValueRefs = deps.getLeft();
    final List<SqlNode> depColRefs = deps.getRight();
    depValueRefs.addAll(0, valueRefs);
    depColRefs.addAll(0, lhsExpr.colRefs());

    final Expression inSubExpr = Expression.mk(inSubExprAst, depColRefs);
    plan.infoCache().putSubqueryExprOf(nodeId, inSubExpr);
    valuesReg.bindValueRefs(inSubExpr, depValueRefs);

    return true;
  }

  private boolean bindSort(int nodeId) {
    // Order By can use the attributes exposed in table-source
    // e.g., Select t.x From t Order By t.y
    // So we have to lookup in deeper descendant.
    final int child = plan.childOf(nodeId, 0);
    final int grandChild = plan.childOf(child, 0);
    final PlanKind childKind = plan.kindOf(child);

    final Values secondaryLookup = valuesReg.valuesOf(child);
    final List<Value> primaryLookup;
    if (childKind == PlanKind.Proj) {
      primaryLookup = valuesReg.valuesOf(grandChild);
    } else if (childKind == PlanKind.Agg) {
      primaryLookup = valuesReg.valuesOf(plan.childOf(grandChild, 0));
    } else if (childKind == PlanKind.SetOp) {
      primaryLookup = Collections.emptyList();
    } else {
      return onError(FAILURE_INVALID_PLAN + plan);
    }

    final List<Value> lookup = ListSupport.join(primaryLookup, secondaryLookup);

    for (Expression sortSpec : ((SortNode) plan.nodeAt(nodeId)).sortSpec()) {
      final List<Value> refs = ListSupport.map(sortSpec.colRefs(), it -> bindRef(it, lookup));
      if (refs.contains(null)) return false;
      valuesReg.bindValueRefs(sortSpec, refs);
    }

    return true;
  }

  private Value bindRef(SqlNode colRef, List<Value> lookup) {
    final SqlNode colName = colRef.$(ColRef_ColName);
    final String qualification = colName.$(ColName_Table);
    final String name = colName.$(ColName_Col);

    for (Value value : lookup) {
      if ((qualification == null || qualification.equalsIgnoreCase(value.qualification()))
          && name.equalsIgnoreCase(value.name())) {
        return value;
      }
    }

    onError(FAILURE_MISSING_REF + colRef);
    return null;
  }

  private boolean onError(String error) {
    this.error = error;
    return false;
  }

  private SqlNode mkInSubExpr(int nodeId) {
    final SqlNode query = translateAsAst(plan, plan.childOf(nodeId, 1), true);
    if (query == null) return null;

    final SqlContext sqlCtx = query.context();
    final SqlNode rhsExpr = SqlSupport.mkQueryExpr(sqlCtx, query);
    final SqlNode lhsExpr = copyAst(((InSubNode) plan.nodeAt(nodeId)).expr().template(), sqlCtx);
    return SqlSupport.mkBinary(sqlCtx, IN_SUBQUERY, lhsExpr, rhsExpr);
  }

  private SqlNode mkExistsExpr(int nodeId) {
    final SqlNode query = translateAsAst(plan, plan.childOf(nodeId, 1), true);
    if (query == null) return null;

    final SqlContext sqlCtx = query.context();
    final SqlNode queryExpr = SqlSupport.mkQueryExpr(sqlCtx, query);
    final SqlNode exists = SqlNode.mk(sqlCtx, Exists);
    return exists.$(Exists_Subquery, queryExpr);
  }

  private static boolean isPlainInSub(InSubNode inSub) {
    final SqlNode expr = inSub.expr().template();
    if (ColRef.isInstance(expr)) return true;
    if (!Tuple.isInstance(expr)) return false;
    return all(expr.$(Tuple_Exprs), ColRef::isInstance);
  }
}
