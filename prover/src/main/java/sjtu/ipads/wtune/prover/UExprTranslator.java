package sjtu.ipads.wtune.prover;

import static java.util.Objects.requireNonNull;
import static sjtu.ipads.wtune.common.utils.Commons.coalesce;
import static sjtu.ipads.wtune.common.utils.FuncUtils.arrayMap;
import static sjtu.ipads.wtune.prover.expr.UExpr.add;
import static sjtu.ipads.wtune.prover.expr.UExpr.eqPred;
import static sjtu.ipads.wtune.prover.expr.UExpr.mul;
import static sjtu.ipads.wtune.prover.expr.UExpr.not;
import static sjtu.ipads.wtune.prover.expr.UExpr.sum;
import static sjtu.ipads.wtune.prover.expr.UExpr.uninterpretedPred;
import static sjtu.ipads.wtune.prover.utils.Constants.FREE_VAR;
import static sjtu.ipads.wtune.prover.utils.Constants.NOT_NULL_PRED;
import static sjtu.ipads.wtune.prover.utils.Constants.TRANSLATOR_VAR_PREFIX;
import static sjtu.ipads.wtune.sqlparser.ast.ExprFields.BINARY_LEFT;
import static sjtu.ipads.wtune.sqlparser.ast.ExprFields.BINARY_RIGHT;
import static sjtu.ipads.wtune.sqlparser.ast.constants.ExprKind.LITERAL;
import static sjtu.ipads.wtune.sqlparser.plan.OperatorType.InnerJoin;
import static sjtu.ipads.wtune.sqlparser.plan.OperatorType.LeftJoin;
import static sjtu.ipads.wtune.sqlparser.plan.OperatorType.Proj;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import sjtu.ipads.wtune.prover.expr.Tuple;
import sjtu.ipads.wtune.prover.expr.UExpr;
import sjtu.ipads.wtune.prover.utils.Constants;
import sjtu.ipads.wtune.sqlparser.ast.ASTNode;
import sjtu.ipads.wtune.sqlparser.ast.constants.SetOperation;
import sjtu.ipads.wtune.sqlparser.plan1.Expr;
import sjtu.ipads.wtune.sqlparser.plan1.InSubFilter;
import sjtu.ipads.wtune.sqlparser.plan1.InputNode;
import sjtu.ipads.wtune.sqlparser.plan1.JoinNode;
import sjtu.ipads.wtune.sqlparser.plan1.PlainFilterNode;
import sjtu.ipads.wtune.sqlparser.plan1.PlanContext;
import sjtu.ipads.wtune.sqlparser.plan1.PlanNode;
import sjtu.ipads.wtune.sqlparser.plan1.ProjNode;
import sjtu.ipads.wtune.sqlparser.plan1.Ref;
import sjtu.ipads.wtune.sqlparser.plan1.RefBag;
import sjtu.ipads.wtune.sqlparser.plan1.SetOpNode;
import sjtu.ipads.wtune.sqlparser.plan1.Value;
import sjtu.ipads.wtune.sqlparser.plan1.ValueBag;

public class UExprTranslator {
  private final PlanNode plan;
  private final PlanContext ctx;

  private final List<ProjNode> scopes; // Proj is treated as the boundary of a query
  private final List<Tuple> pivotTuples; // each query owns a free tuple

  private Tuple inSubqueryLhs; // for IN-subquery

  private int nextTupleIdx = 0;
  private int nextAnonAttrIdx = 0;

  UExprTranslator(PlanNode plan) {
    this.plan = plan;
    this.ctx = requireNonNull(plan.context());
    this.scopes = new ArrayList<>(5);
    this.pivotTuples = new ArrayList<>(5);
    this.pivotTuples.add(Tuple.make(FREE_VAR));
  }

  public static UExpr translate(PlanNode plan) {
    return new UExprTranslator(plan).onNode(plan);
  }

  private UExpr onNode(PlanNode node) {
    switch (node.type()) {
      case Proj:
        return onProj((ProjNode) node);
      case Input:
        return onInput((InputNode) node);
      case InnerJoin:
      case LeftJoin:
        return onJoin((JoinNode) node);
      case PlainFilter:
        return onPlainFilter((PlainFilterNode) node);
      case InSubFilter:
        return onSubqueryFilter((InSubFilter) node);
      case Union:
        return onUnion((SetOpNode) node);
      case Agg: // TODO: come up with how to deal Agg
      case Sort: // ignore Sort and Limit
      case Limit:
        return onNode(node.predecessors()[0]);
      default:
        throw new IllegalArgumentException("unsupported operator type " + node.type());
    }
  }

  private UExpr onInput(InputNode input) {
    final String qualification = input.values().qualification();
    final String tableName = input.table().name();
    return UExpr.table(tableName, currentPivot().proj(qualification));
  }

  private UExpr onProj(ProjNode proj) {
    pushScope(proj);

    final ValueBag values = proj.values();
    // TODO: relax such limitation in the future
    if (inSubqueryLhs != null && values.size() != 1) throw failed("invalid IN-subquery");

    final List<UExpr> factors = new ArrayList<>(values.size() + 1);
    // each select item {a.x AS b} turns into a predicate [t'.a.x = t.b],
    // where t' is current pivot tuple, t is the outer pivot tuple
    for (Value value : values) { // `value` must be ExprValue
      final Expr expr = value.expr();
      if (expr == null) throw failed("invalid selection: " + value);

      // project on outer pivot if this is not RHS of a IN-subquery
      // otherwise, use the `inSubqueryLhs`
      final Tuple outer = coalesce(inSubqueryLhs, () -> projTuple(outerPivot(), value));
      final Tuple inner = makeTuple(expr);

      factors.add(eqPred(outer, inner));
    }

    factors.add(onNode(proj.predecessors()[0])); // add sub-expression

    UExpr expr =
        factors.stream()
            .reduce(UExpr::mul)
            .map(it -> UExpr.sum(currentPivot(), it))
            .orElseThrow(() -> failed("null expression for " + proj));

    if (proj.isExplicitDistinct()) expr = UExpr.squash(expr);

    popScope();

    return expr;
  }

  private UExpr onPlainFilter(PlainFilterNode filter) {
    final Expr predicate = filter.predicate();

    final UExpr condExpr;
    if (predicate.isJoinCondition()) {
      final RefBag refs = predicate.refs();
      assert refs.size() == 2;

      final Tuple lhs = projTuple(refs.get(0));
      final Tuple rhs = projTuple(refs.get(1));
      condExpr = mul(mul(eqPred(lhs, rhs), notNullPred(lhs)), notNullPred(rhs));

    } else if (predicate.isEquiCondition()) {
      final RefBag refs = predicate.refs();
      assert refs.size() == 1;

      final Tuple lhsTuple = projTuple(refs.get(0));
      final Tuple rhsTuple;

      final ASTNode lhsExpr = predicate.template().get(BINARY_LEFT);
      final ASTNode rhsExpr = predicate.template().get(BINARY_RIGHT);
      if (LITERAL.isInstance(lhsExpr)) rhsTuple = Tuple.constant(lhsExpr.toString());
      else rhsTuple = Tuple.constant(rhsExpr.toString());

      condExpr = eqPred(lhsTuple, rhsTuple);

    } else {
      condExpr = makeUninterpretedPred(predicate);
    }

    final UExpr remaining = onNode(filter.predecessors()[0]);
    return mul(condExpr, remaining);
  }

  private UExpr onSubqueryFilter(InSubFilter filter) {
    final UExpr lhsExpr = onNode(filter.predecessors()[0]);

    inSubqueryLhs = makeTuple(filter.lhsExpr());
    final UExpr rhsExpr = onNode(filter.predecessors()[1]);
    inSubqueryLhs = null;

    return mul(lhsExpr, UExpr.squash(rhsExpr));
  }

  private UExpr onJoin(JoinNode join) {
    final UExpr lhsExpr = onNode(join.predecessors()[0]);
    final UExpr rhsExpr = onNode(join.predecessors()[1]);
    if (join.condition() == null) return mul(lhsExpr, rhsExpr);

    final UExpr condition;
    if (join.isEquiJoin()) {
      final RefBag lhsRefs = join.lhsRefs(), rhsRefs = join.rhsRefs();
      assert lhsRefs.size() == rhsRefs.size();

      final List<UExpr> conditions = new ArrayList<>(lhsRefs.size());
      for (int i = 0, bound = lhsRefs.size(); i < bound; ++i) {
        final Tuple lhs = projTuple(lhsRefs.get(i));
        final Tuple rhs = projTuple(rhsRefs.get(i));
        conditions.add(eqPred(lhs, rhs));
        //        conditions.add(notNullPred(lhs)); // one-side is enough
        conditions.add(notNullPred(rhs));
      }
      condition = conditions.stream().reduce(UExpr::mul).orElse(null);

    } else {
      condition = makeUninterpretedPred(join.condition());
    }
    // L(x) * R(y) * p(x,y)
    final UExpr symmPart = mul(mul(condition, lhsExpr), rhsExpr);

    if (join.type() == InnerJoin) return symmPart;

    if (join.type() == LeftJoin) {
      // [y = null] * Sum{y'}(R(y') * p(x,y'))
      final UExpr asymmPart = makeAsymmetricJoin(join.predecessors()[1], condition, rhsExpr);
      // L(x) * R(y) * p(x,y) +
      // L(x) * [y = null] * Sum{y'}(R(y') * p(x,y'))
      return add(symmPart, mul(lhsExpr.copy(), asymmPart));
    }

    throw new IllegalArgumentException("unsupported join type: " + join.type());
  }

  private UExpr onUnion(SetOpNode setOp) {
    final UExpr lhsExpr = onNode(setOp.predecessors()[0]);
    final UExpr rhsExpr = onNode(setOp.predecessors()[1]);

    // TODO: support other set operation
    if (setOp.operation() != SetOperation.UNION)
      throw failed("unsupported set operation: " + setOp.operation());

    return UExpr.add(lhsExpr, rhsExpr);
  }

  private void pushScope(ProjNode proj) {
    scopes.add(proj);
    pivotTuples.add(Tuple.make(TRANSLATOR_VAR_PREFIX + nextTupleIdx++));
  }

  private void popScope() {
    scopes.remove(scopes.size() - 1);
    pivotTuples.remove(pivotTuples.size() - 1);
  }

  private Tuple currentPivot() {
    return pivotTuples.get(pivotTuples.size() - 1);
  }

  private Tuple outerPivot() {
    return pivotTuples.get(pivotTuples.size() - 2);
  }

  private Tuple projTuple(Tuple base, Value v) {
    final String qualification = v.qualification();
    final String name = v.name().isEmpty() ? "_" + nextAnonAttrIdx++ : v.name();
    if (qualification == null) return base.proj(name);
    else return base.proj(qualification).proj(name);
  }

  private Tuple projTuple(Ref ref) {
    final Value value = ctx.deRef(ref);
    final ProjNode scope = scopeOf(value);

    final int idx = scopes.lastIndexOf(scope); // backward search
    if (idx == -1) throw failed("cannot locate the depended tuple: " + ref);

    return projTuple(pivotTuples.get(idx + 1), value);
  }

  private Tuple[] projTuples(Collection<Ref> refs) {
    return arrayMap(this::projTuple, Tuple.class, refs);
  }

  private Tuple makeTuple(Expr expr) {
    final RefBag refs = expr.refs();
    if (expr.isIdentity()) {
      assert refs.size() == 1;
      return projTuple(refs.get(0));
    } else {
      return Tuple.func(expr.template().toString(), projTuples(refs));
    }
  }

  private UExpr notNullPred(Tuple tuple) {
    return uninterpretedPred(NOT_NULL_PRED, tuple);
  }

  private UExpr isNullPred(Tuple tuple) {
    return eqPred(tuple, Constants.NULL_TUPLE);
  }

  private UExpr makeUninterpretedPred(Expr expr) {
    final Tuple[] args = projTuples(expr.refs());
    return UExpr.uninterpretedPred(expr.template().toString(), args);
  }

  private UExpr makeAsymmetricJoin(PlanNode asymmJoin, UExpr cond, UExpr rhsExpr) {
    // A LEFT JOIN B ON p(A.a,B.b) =>
    // Sum{x,y}(A(x) * B(y) * [p(x.a,y.b)] * [NotNull(x.a)] * [NotNull(y.b)]) -- symmetric part
    // + Sum{x}(A(x) * [IsNull(y)] * not(Sum{y}(B(y) * [p(x.a,y.b)]))) -- asymmetric part
    // this method returns the part "[IsNull(y)] * not(Sum{y'}(B(y') * p(x.a,y'.b))

    cond = cond.copy();
    rhsExpr = rhsExpr.copy();

    // Usually the RHS is a single source (a plain or a subquery),
    // but we handle the most general case.
    final List<String> rhsTables =
        asymmJoin.values().stream().map(Value::qualification).distinct().toList();
    if (rhsTables.isEmpty()) throw failed("invalid join: " + asymmJoin.successor());

    final Tuple pivotVar = currentPivot();
    final Tuple freeVar = Tuple.make(TRANSLATOR_VAR_PREFIX + nextTupleIdx++);

    UExpr isNullPred = null;
    for (String tableAlias : rhsTables) {
      final Tuple oldTup = pivotVar.proj(tableAlias);
      final Tuple newTup = freeVar.proj(tableAlias);

      final UExpr isNullPred0 = isNullPred(oldTup);
      isNullPred = isNullPred == null ? isNullPred0 : mul(isNullPred, isNullPred0);

      cond.subst(oldTup, newTup);
      rhsExpr.subst(oldTup, newTup);
    }

    return mul(isNullPred, not(sum(freeVar, mul(cond, rhsExpr))));
  }

  private RuntimeException failed(String reason) {
    return new IllegalArgumentException(
        "failed to translate plan to U-expr. [" + reason + "] " + plan);
  }

  private ProjNode scopeOf(Value value) {
    final PlanNode owner = ctx.ownerOf(value);
    PlanNode scope = owner.successor();
    while (scope != null) {
      if (scope.type() == Proj) return (ProjNode) scope;
      scope = scope.successor();
    }
    throw failed("cannot find the scope of value " + value);
  }

  private static class QueryScope {
    private Tuple outerVar;
    private List<Tuple> vars;
  }
}
