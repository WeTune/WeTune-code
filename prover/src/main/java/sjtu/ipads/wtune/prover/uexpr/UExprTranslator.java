package sjtu.ipads.wtune.prover.uexpr;

import sjtu.ipads.wtune.common.utils.NameSequence;
import sjtu.ipads.wtune.prover.utils.UExprUtils;
import sjtu.ipads.wtune.sqlparser.ast.ASTNode;
import sjtu.ipads.wtune.sqlparser.ast.constants.SetOperation;
import sjtu.ipads.wtune.sqlparser.plan.OperatorType;
import sjtu.ipads.wtune.sqlparser.plan1.*;

import java.util.*;
import java.util.stream.Stream;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Objects.requireNonNull;
import static sjtu.ipads.wtune.common.utils.Commons.elemAt;
import static sjtu.ipads.wtune.common.utils.FuncUtils.*;
import static sjtu.ipads.wtune.prover.uexpr.UExpr.*;
import static sjtu.ipads.wtune.prover.uexpr.Var.mkBase;
import static sjtu.ipads.wtune.prover.uexpr.Var.mkConstant;
import static sjtu.ipads.wtune.prover.utils.Constants.TRANSLATOR_VAR_PREFIX;
import static sjtu.ipads.wtune.prover.utils.UExprUtils.mkNotNull;
import static sjtu.ipads.wtune.prover.utils.UExprUtils.mkProduct;
import static sjtu.ipads.wtune.sqlparser.ast.ExprFields.BINARY_LEFT;
import static sjtu.ipads.wtune.sqlparser.ast.ExprFields.BINARY_RIGHT;
import static sjtu.ipads.wtune.sqlparser.ast.constants.ExprKind.LITERAL;
import static sjtu.ipads.wtune.sqlparser.plan.OperatorType.*;

public class UExprTranslator {
  private static final Var ROOT_VAR = Var.mkBase(TRANSLATOR_VAR_PREFIX);

  private final PlanNode plan;
  private final PlanContext ctx;

  private final List<QueryScope> scopes;
  private final Map<PlanNode, Var> varOwnership;

  private final NameSequence varNameSeq;

  UExprTranslator(PlanNode plan) {
    this.plan = plan;
    this.ctx = requireNonNull(plan.context());
    this.scopes = new ArrayList<>(4);
    this.varOwnership = new HashMap<>(8);
    this.varNameSeq = NameSequence.mkIndexed(TRANSLATOR_VAR_PREFIX, 0);

    pushScope();
  }

  public static UExpr translate(PlanNode plan) {
    return new UExprTranslator(plan).onNode(plan);
  }

  private UExpr onNode(PlanNode node) {
    // TODO: come up with how to deal Agg
    // Note: some weird query like (Select Sum ...) Union (...) cannot be handled for now.
    return switch (node.kind()) {
      case PROJ -> onProj((ProjNode) node);
      case INPUT -> onInput((InputNode) node);
      case INNER_JOIN, LEFT_JOIN -> onJoin((JoinNode) node);
      case SIMPLE_FILTER -> onPlainFilter((SimpleFilterNode) node);
      case IN_SUB_FILTER -> onInSubFilter((InSubFilterNode) node);
      case EXISTS_FILTER -> onExistsFilter((ExistsFilterNode) node);
      case UNION -> onUnion((SetOpNode) node);
      case AGG, SORT, LIMIT -> onNode(node.predecessors()[0]);
    };
  }

  private UExpr onInput(InputNode input) {
    final List<Var> joints = localScope().joints();

    if (joints == null) {
      return table(input.table().name(), localScope().mkVar(input));
    } else {
      final Var var = mkBase(varNameSeq.next());
      final UExpr tableTerm = table(input.table().name(), var);
      final UExpr term =
          joints.stream()
              .map(it -> eqPred(it, var.proj("_" + it.name())))
              .reduce(tableTerm, UExpr::mul);
      return sum(var, term);
    }
  }

  private UExpr onProj(ProjNode proj) {
    pushScope();

    final QueryScope outerScope = outerScope();
    final ValueBag vs = proj.values();
    final List<Var> joints;
    if (outerScope.joints() != null) {
      // this branch is for 1. the root query 2. the subquery in a Union
      // 3. the subquery in an IN-Sub/Exists Filter
      joints = outerScope.joints;
    } else {
      // this branch is for the subquery in From/Join
      final Var pivotVar = outerScope.mkVar(proj);
      joints = listMap(vs, it -> pivotVar.proj(it.name()));
    }

    // Each select item {a.x AS b} turns into a predicate [a.x = t.b],
    // where `a` is var for table `a`, `t` is the outer var.
    final List<UExpr> terms = new ArrayList<>(vs.size() + 1);
    terms.add(onNode(proj.predecessors()[0]));
    terms.addAll(zipMap(joints, vs, (o, v) -> eqPred(o, asTuple(v.expr()))));

    final UExpr expr = sum(localScope().localVars(), mkProduct(terms));

    popScope();

    return proj.isDeduplicated() ? squash(expr) : expr;
  }

  private UExpr onPlainFilter(SimpleFilterNode filter) {
    final UExpr remaining = onNode(filter.predecessors()[0]);

    final Expr predicate = filter.predicate();
    final UExpr condExpr;
    if (predicate.isJoinCondition()) {
      final RefBag refs = predicate.refs();
      assert refs.size() == 2;
      condExpr = UExprUtils.mkNullSafeEq(asTuple(refs.get(0)), asTuple(refs.get(1)));

    } else if (predicate.isEquiCondition()) {
      final RefBag refs = predicate.refs();
      assert refs.size() == 1;

      final ASTNode lhsExpr = predicate.template().get(BINARY_LEFT);
      final ASTNode rhsExpr = predicate.template().get(BINARY_RIGHT);
      final Var lhs = asTuple(refs.get(0));
      final Var rhs = mkConstant((LITERAL.isInstance(lhsExpr) ? lhsExpr : rhsExpr).toString());
      condExpr = eqPred(lhs, rhs);

    } else {
      condExpr = mkUninterpretedPred(predicate);
    }

    return mul(condExpr, remaining);
  }

  private UExpr onInSubFilter(InSubFilterNode filter) {
    final UExpr lhs = onNode(filter.predecessors()[0]);
    final List<Var> joints = asList(asTuples(filter.lhsRefs()));
    localScope().setJoints(joints);
    final UExpr rhs = onNode(filter.predecessors()[1]);
    localScope().setJoints(null);

    final Stream<UExpr> nonNullCond = joints.stream().map(UExprUtils::mkNotNull);
    return mul(nonNullCond.reduce(lhs, UExpr::mul), squash(rhs));
  }

  private UExpr onExistsFilter(ExistsFilterNode filter) {
    final UExpr lhs = onNode(filter.predecessors()[0]);
    localScope().setJoints(emptyList());
    final UExpr rhs = onNode(filter.predecessors()[1]);
    localScope().setJoints(null);
    return mul(lhs, UExpr.squash(rhs));
  }

  private UExpr onJoin(JoinNode join) {
    final UExpr lhsExpr = onNode(join.predecessors()[0]);
    final UExpr rhsExpr = onNode(join.predecessors()[1]);

    if (join.condition() == null) return mul(lhsExpr, rhsExpr);

    final UExpr mainCond;
    final UExpr notNullCond;
    if (join.isEquiJoin()) {
      final RefBag lhs = join.lhsRefs(), rhs = join.rhsRefs();
      assert lhs.size() == rhs.size();
      mainCond = mkProduct(zipMap(lhs, rhs, (x, y) -> eqPred(asTuple(x), asTuple(y))));
      notNullCond = mkProduct(listMap(rhs, x -> mkNotNull(asTuple(x))));
    } else {
      mainCond = mkUninterpretedPred(join.condition());
      notNullCond = null;
    }
    // L(x) * R(y) * p(x,y)
    final UExpr symmPart = mul(mul(mul(lhsExpr, rhsExpr), mainCond), notNullCond);

    if (join.kind() == INNER_JOIN) return symmPart;

    if (join.kind() == LEFT_JOIN) {
      // L(x) * [y = 0] * not(Sum{y'}(R(y') * p(x,y')))
      final UExpr asymmPart = mkAsymmetricJoin(join, mainCond, notNullCond, lhsExpr, rhsExpr);
      // L(x) * R(y) * p(x,y) +
      // L(x) * [y = 0] * not(Sum{y'}(R(y') * p(x,y')))
      return add(symmPart, asymmPart);
    }

    throw new IllegalArgumentException("unsupported join type: " + join.kind());
  }

  private UExpr onUnion(SetOpNode setOp) {
    // TODO: support other set operation
    if (setOp.operation() != SetOperation.UNION)
      throw failed("unsupported set operation: " + setOp.operation());

    // The root of union tree is responsible to create the join var.
    // e.g., Select sub.a From ((Select A.a From A) Union (Select B.b From B)) As sub
    // => Sum{x}(Sum{y}([x.a = y.a] * A(y)) + Sum{z}([x.a = z.b] * B(z)))

    final QueryScope localScope = localScope();
    final boolean isUnionHead = isUnionHead(setOp);

    if (isUnionHead) {
      final ValueBag values = setOp.values();
      final Var joint = localScope.mkVar(ctx.ownerOf(values.get(0)));
      localScope.setJoints(listMap(values, it -> joint.proj(it.name())));
    }

    final UExpr lhsExpr = onNode(setOp.predecessors()[0]);
    final UExpr rhsExpr = onNode(setOp.predecessors()[1]);

    if (isUnionHead) localScope.setJoints(null);

    if (setOp.distinct()) return squash(add(lhsExpr, rhsExpr));
    else return add(lhsExpr, rhsExpr);
  }

  private void pushScope() {
    scopes.add(new QueryScope(scopes.isEmpty()));
  }

  private void popScope() {
    scopes.remove(scopes.size() - 1);
  }

  private QueryScope outerScope() {
    return elemAt(scopes, -2);
  }

  private QueryScope localScope() {
    return elemAt(scopes, -1);
  }

  private Var asTuple(Ref ref) {
    final Value v = ctx.deRef(ref);
    final Var var = varOwnership.get(ctx.ownerOf(v));
    return var.proj(v.name());
  }

  private Var asTuple(Expr expr) {
    final RefBag refs = expr.refs();
    if (expr.isIdentity()) {
      assert refs.size() == 1;
      return asTuple(refs.get(0));
    } else {
      return Var.mkFunc(expr.template().toString(), asTuples(refs));
    }
  }

  private Var[] asTuples(Collection<Ref> refs) {
    return arrayMap(refs, this::asTuple, Var.class);
  }

  private UExpr mkUninterpretedPred(Expr expr) {
    final Var[] args = asTuples(expr.refs());
    return UExpr.uninterpretedPred(expr.template().toString(), args);
  }

  private UExpr mkAsymmetricJoin(
      PlanNode asymmJoin, UExpr mainCond, UExpr notNullCond, UExpr lhsExpr, UExpr rhsExpr) {
    // A LEFT JOIN B ON p(A.a,B.b) =>
    //  -- symmetric part
    // A(x) * B(y) * [p(x.a,y.b)] * [x != NULL] * [y != NULL]
    //  -- asymmetric part
    // + A(x) * [y = NULL] * not(Sum{z}(B(z) * [p(x.a,z.b)] * [x != NULL] * [z != NULL])

    //    UExpr cond0 = mainCond.copy();
    UExpr cond1 = mul(mainCond.copy(), notNullCond.copy());
    lhsExpr = lhsExpr.copy();
    rhsExpr = rhsExpr.copy();

    // We need to find the vars from `rhsExpr` and replaces them with fresh vars to be used in Sum.
    // Usually RHS is a single plain/subquery source, but let's handle the most general case.
    final List<Var> oldVars =
        asymmJoin.predecessors()[1].values().stream()
            .map(ctx::ownerOf) // The owner of the values.
            .map(varOwnership::get) // The corresponding vars of the owners.
            .distinct()
            .toList();

    final List<Var> newVars = new ArrayList<>(oldVars.size());
    final List<UExpr> isNullPreds = new ArrayList<>(oldVars.size());

    for (Var oldVar : oldVars) {
      final Var newVar = mkBase(varNameSeq.next());
      newVars.add(newVar);
      isNullPreds.add(UExprUtils.mkIsNull(oldVar));

      //      cond0.subst(oldVar, NULL);
      cond1.subst(oldVar, newVar);
      rhsExpr.subst(oldVar, newVar);
    }

    //    return mul(
    //        mul(lhsExpr, mkProduct(isNullPreds)), add(cond0, not(sum(newVars, mul(rhsExpr,
    // cond1)))));
    return mul(mul(lhsExpr, mkProduct(isNullPreds)), not(sum(newVars, mul(rhsExpr, cond1))));
  }

  private static boolean isUnionHead(SetOpNode node) {
    // if the query the left-most query in a Union
    // returns true if `node` is not a union component
    PlanNode n = node;

    while (n.successor() != null) {
      final PlanNode succ = n.successor();
      final OperatorType succType = succ.kind();

      if (succType == UNION) return false;
      if (succType == SORT || succType == LIMIT) n = succ;
      else return true;
    }

    return true;
  }

  private RuntimeException failed(String reason) {
    return new IllegalArgumentException(
        "failed to translate plan to U-expr. [" + reason + "] " + plan);
  }

  private class QueryScope {
    protected boolean isRoot;
    protected final List<Var> localVars;
    protected List<Var> joints;
    // `hinge` is the connection between current query to a subquery
    // Currently it is used in "`outer_col` IN <subquery>"

    private QueryScope(boolean isRoot) {
      this.isRoot = isRoot;
      this.localVars = new ArrayList<>(4);
    }

    private List<Var> localVars() {
      return localVars;
    }

    private List<Var> joints() {
      return joints;
    }

    private void setJoints(List<Var> joints) {
      this.joints = joints;
    }

    private Var mkVar(PlanNode owner) {
      if (isRoot) return ROOT_VAR;

      final Var var = Var.mkBase(varNameSeq.next());
      varOwnership.put(owner, var);
      localVars.add(var);
      return var;
    }
  }
}
