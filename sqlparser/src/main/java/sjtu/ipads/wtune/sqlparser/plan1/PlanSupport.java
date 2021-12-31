package sjtu.ipads.wtune.sqlparser.plan1;

import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import sjtu.ipads.wtune.common.utils.NameSequence;
import sjtu.ipads.wtune.sqlparser.ast1.SqlContext;
import sjtu.ipads.wtune.sqlparser.ast1.SqlNode;
import sjtu.ipads.wtune.sqlparser.ast1.SqlNodes;
import sjtu.ipads.wtune.sqlparser.ast1.constants.BinaryOpKind;
import sjtu.ipads.wtune.sqlparser.ast1.constants.JoinKind;
import sjtu.ipads.wtune.sqlparser.ast1.constants.LiteralKind;
import sjtu.ipads.wtune.sqlparser.schema.Column;
import sjtu.ipads.wtune.sqlparser.schema.Schema;
import sjtu.ipads.wtune.sqlparser.schema.Table;

import java.util.*;

import static sjtu.ipads.wtune.sqlparser.SqlSupport.*;
import static sjtu.ipads.wtune.sqlparser.ast1.ExprFields.*;
import static sjtu.ipads.wtune.sqlparser.ast1.ExprKind.*;
import static sjtu.ipads.wtune.sqlparser.plan1.PlanKind.*;

public abstract class PlanSupport {
  public static final String FAILURE_INVALID_QUERY = "invalid query ";
  public static final String FAILURE_INVALID_PLAN = "invalid plan ";
  public static final String FAILURE_UNSUPPORTED_FEATURE = "unsupported feature ";
  public static final String FAILURE_UNKNOWN_TABLE = "unknown table ";
  public static final String FAILURE_MISSING_PROJECTION = "missing projection ";
  public static final String FAILURE_MISSING_QUALIFICATION = "missing qualification ";
  public static final String FAILURE_MISSING_REF = "missing ref ";
  public static final String FAILURE_BAD_SUBQUERY_EXPR = "bad subquery expr ";

  static final String SYN_NAME_PREFIX = "%";
  static final String PLACEHOLDER_NAME = "#";

  private static final ThreadLocal<String> LAST_ERROR = new ThreadLocal<>();

  private PlanSupport() {}

  public static boolean isSupported(SqlNode ast) {
    final SqlContext ctx = ast.context();
    for (int i = 1; i <= ctx.maxNodeId(); i++)
      if (ctx.isPresent(i) && ctx.fieldOf(i, Aggregate_WindowSpec) != null) {
        return false;
      }
    return true;
  }

  /** Build a plan tree from AST. If `schema` is null then fallback to ast.context().schema() */
  public static PlanContext buildPlan(SqlNode ast, Schema schema) {
    final PlanBuilder builder = new PlanBuilder(ast, schema);
    if (builder.build()) return builder.plan();
    else {
      LAST_ERROR.set(builder.lastError());
      return null;
    }
  }

  /** Set up values and resolve the value refs. */
  public static PlanContext resolvePlan(PlanContext plan) {
    final ValueRefBinder binder = new ValueRefBinder(plan);
    if (binder.bind()) return plan;
    else {
      LAST_ERROR.set(binder.lastError());
      return null;
    }
  }

  /**
   * Build a plan tree from AST, set up values and resolve the value refs. If `schema` is null then
   * fallback to ast.context().schema()
   */
  public static PlanContext assemblePlan(SqlNode ast, Schema schema) {
    PlanContext plan;
    if ((plan = buildPlan(ast, schema)) == null) return null;
    if ((plan = resolvePlan(plan)) == null) return null;
    return disambiguateQualification(plan);
  }

  public static SqlNode translateAsAst(PlanContext context, int nodeId, boolean allowIncomplete) {
    final ToAstTranslator translator = new ToAstTranslator(context);
    final SqlNode sql = translator.translate(nodeId, allowIncomplete);
    if (sql != null) return sql;
    else {
      LAST_ERROR.set(translator.lastError());
      return null;
    }
  }

  public static int locateNode(PlanContext ctx, int rootId, int... pathExpr) {
    int node = rootId;
    for (int direction : pathExpr) node = ctx.childOf(node, direction);
    return node;
  }

  public static String getLastError() {
    return LAST_ERROR.get();
  }

  public static boolean isEqualTree(PlanContext ctx0, int tree0, PlanContext ctx1, int tree1) {
    // Now we only support compare two Input nodes.
    final PlanNode node0 = ctx0.nodeAt(tree0), node1 = ctx1.nodeAt(tree1);
    if (node0.kind() != Input || node1.kind() != Input) return false;
    final Table t0 = ((InputNode) node0).table(), t1 = ((InputNode) node1).table();
    return t0.equals(t1);
  }

  public static String stringifyNode(PlanContext ctx, int id) {
    return PlanStringifier.stringifyNode(ctx, id);
  }

  public static String stringifyTree(PlanContext ctx, int id) {
    return PlanStringifier.stringifyTree(ctx, id);
  }

  public static boolean isRootRef(PlanContext ctx, Value value) {
    return ctx.valuesReg().exprOf(value) == null;
  }

  /**
   * Returns the direct ref of the value. Returns null if the value is a base value (i.e., directly
   * derives from a table source) or is not a ColRef (i.e., a complex expression)
   */
  public static Value tryResolveRef(PlanContext ctx, Value value) {
    return tryResolveRef(ctx, value, false);
  }

  public static Value tryResolveRef(PlanContext ctx, Value value, boolean recursive) {
    final ValuesRegistry valueReg = ctx.valuesReg();
    final Expression expr = valueReg.exprOf(value);
    if (expr == null) return null;
    if (!isColRef(expr)) return null;

    final Values refs = valueReg.valueRefsOf(expr);
    assert refs.size() == 1;
    return recursive ? tryResolveRef(ctx, refs.get(0), true) : refs.get(0);
  }

  public static Column tryResolveColumn(PlanContext ctx, Value value) {
    final ValuesRegistry valueReg = ctx.valuesReg();
    final Expression expr = valueReg.exprOf(value);
    if (expr == null) return valueReg.columnOf(value);
    if (!isColRef(expr)) return null;

    final Values refs = valueReg.valueRefsOf(expr);
    assert refs.size() == 1;
    return tryResolveColumn(ctx, refs.get(0));
  }

  public static JoinKind joinKindOf(PlanContext ctx, int nodeId) {
    if (ctx.kindOf(nodeId) != Join) return null;
    final JoinKind joinKind = ctx.infoCache().getJoinKindOf(nodeId);
    if (joinKind != null) return joinKind;
    return ((JoinNode) ctx.nodeAt(nodeId)).joinKind();
  }

  public static boolean isUniqueCoreAt(PlanContext ctx, Collection<Value> attrs, int surfaceId) {
    return new UniquenessInference(ctx).isUniqueCoreAt(attrs, surfaceId);
  }

  public static boolean isNotNullAt(PlanContext ctx, Value attrs, int surfaceId) {
    return new NotNullInference(ctx).isNotNullAt(attrs, surfaceId);
  }

  // Must be invoked after `resolvePlan`
  public static PlanContext disambiguateQualification(PlanContext ctx) {
    final List<PlanNode> nodes = gatherNodes(ctx, EnumSet.of(Proj, Agg, Input));
    final Set<String> knownQualifications = new HashSet<>(nodes.size());
    final NameSequence seq = NameSequence.mkIndexed("q", 0);

    for (PlanNode node : nodes) {
      assert node instanceof Qualified;
      final Qualified qualified = (Qualified) node;

      if (!mustBeQualified(ctx, ctx.nodeIdOf(node))) continue;
      final String oldQualification = qualified.qualification();
      if (oldQualification != null && knownQualifications.add(qualified.qualification())) continue;

      final String newQualification = seq.nextUnused(knownQualifications);
      knownQualifications.add(newQualification);

      qualified.setQualification(newQualification);
      for (Value value : ctx.valuesOf(node)) value.setQualification(newQualification);
    }

    return ctx;
  }

  static List<PlanNode> gatherNodes(PlanContext ctx, PlanKind kind) {
    final List<PlanNode> inputs = new ArrayList<>(8);

    for (int i = 0, bound = ctx.maxNodeId(); i <= bound; i++)
      if (ctx.isPresent(i) && ctx.kindOf(i) == kind) {
        inputs.add(ctx.nodeAt(i));
      }

    return inputs;
  }

  static List<PlanNode> gatherNodes(PlanContext ctx, EnumSet<PlanKind> kinds) {
    final List<PlanNode> inputs = new ArrayList<>(8);

    for (int i = 0, bound = ctx.maxNodeId(); i <= bound; i++)
      if (ctx.isPresent(i) && kinds.contains(ctx.kindOf(i))) {
        inputs.add(ctx.nodeAt(i));
      }

    return inputs;
  }

  //// Expression-related
  public static boolean isColRef(Expression expr) {
    return ColRef.isInstance(expr.template());
  }

  public static Expression mkColRefExpr(Value value) {
    return Expression.mk(mkColRef(SqlContext.mk(2), value.qualification(), value.name()));
  }

  public static Expression mkColRefExpr() {
    return Expression.mk(mkColRef(SqlContext.mk(2), PLACEHOLDER_NAME, PLACEHOLDER_NAME));
  }

  public static Expression mkColRefsExpr(int i) {
    if (i == 1) return mkColRefExpr();

    final SqlContext sqlCtx = SqlContext.mk(i * 2 + 1);
    final TIntList refs = new TIntArrayList(i);
    for (int n = 0; n < i; ++n) {
      final SqlNode ref = mkColRef(sqlCtx, PLACEHOLDER_NAME, PLACEHOLDER_NAME);
      refs.add(ref.nodeId());
    }
    final SqlNodes refNodes = SqlNodes.mk(sqlCtx, refs);

    final SqlNode tuple = SqlNode.mk(sqlCtx, Tuple);
    tuple.$(Tuple_Exprs, refNodes);
    return Expression.mk(tuple);
  }

  public static Expression mkJoinCond(int numKeys) {
    if (numKeys <= 0) throw new IllegalArgumentException();

    final SqlContext sqlCtx = SqlContext.mk(6 * numKeys - 1);

    SqlNode expr = null;
    for (int i = 0; i < numKeys; i++) {
      final SqlNode lhsRef = mkColRef(sqlCtx, PLACEHOLDER_NAME, PLACEHOLDER_NAME);
      final SqlNode rhsRef = mkColRef(sqlCtx, PLACEHOLDER_NAME, PLACEHOLDER_NAME);
      final SqlNode eq = mkBinary(sqlCtx, BinaryOpKind.EQUAL, lhsRef, rhsRef);
      if (expr == null) expr = eq;
      else expr = mkBinary(sqlCtx, BinaryOpKind.AND, expr, eq);
    }
    return Expression.mk(expr);
  }

  static SqlNode normalizePredicate(SqlNode exprAst, SqlContext sqlCtx) {
    if (ColRef.isInstance(exprAst)) {
      final SqlNode literal = mkLiteral(sqlCtx, LiteralKind.BOOL, Boolean.TRUE);
      return mkBinary(sqlCtx, BinaryOpKind.IS, copyAst(exprAst, sqlCtx), literal);
    } else {
      return exprAst;
    }
  }

  static boolean isBoolConstant(SqlNode exprAst) {
    return Binary.isInstance(exprAst)
        && Literal.isInstance(exprAst.$(Binary_Left))
        && Literal.isInstance(exprAst.$(Binary_Right));
  }

  private static boolean mustBeQualified(PlanContext ctx, int nodeId) {
    int parentId = ctx.parentOf(nodeId);
    while (ctx.isPresent(parentId)) {
      final PlanKind parentKind = ctx.kindOf(parentId);
      if (parentKind == SetOp) return false;
      if (parentKind == Proj || parentKind == Join) return true;
      if (parentKind.isFilter()) return ctx.childOf(parentId, 0) == nodeId;
      parentId = ctx.parentOf(parentId);
    }
    return false;
  }
}
