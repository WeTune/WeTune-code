package sjtu.ipads.wtune.sqlparser.plan1;

import sjtu.ipads.wtune.common.utils.NameSequence;
import sjtu.ipads.wtune.common.utils.SetSupport;
import sjtu.ipads.wtune.sqlparser.SqlSupport;
import sjtu.ipads.wtune.sqlparser.ast1.SqlContext;
import sjtu.ipads.wtune.sqlparser.ast1.SqlNode;
import sjtu.ipads.wtune.sqlparser.ast1.constants.BinaryOpKind;
import sjtu.ipads.wtune.sqlparser.ast1.constants.JoinKind;
import sjtu.ipads.wtune.sqlparser.ast1.constants.LiteralKind;
import sjtu.ipads.wtune.sqlparser.schema.Column;
import sjtu.ipads.wtune.sqlparser.schema.Schema;
import sjtu.ipads.wtune.sqlparser.schema.Table;

import java.util.*;

import static java.util.function.Predicate.not;
import static sjtu.ipads.wtune.common.utils.IterableSupport.any;
import static sjtu.ipads.wtune.sqlparser.SqlSupport.*;
import static sjtu.ipads.wtune.sqlparser.ast1.ExprFields.*;
import static sjtu.ipads.wtune.sqlparser.ast1.ExprKind.*;
import static sjtu.ipads.wtune.sqlparser.ast1.constants.ConstraintKind.NOT_NULL;
import static sjtu.ipads.wtune.sqlparser.ast1.constants.ConstraintKind.UNIQUE;
import static sjtu.ipads.wtune.sqlparser.plan1.PlanKind.*;

public abstract class PlanSupport {
  static final String SYN_NAME_PREFIX = "%";

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
    return new PlanBuilder(ast, schema).build();
  }

  /** Set up values and resolve the value refs. */
  public static PlanContext resolvePlan(PlanContext plan) {
    new ValueRefBinder(plan).bind();
    return plan;
  }

  /**
   * Build a plan tree from AST, set up values and resolve the value refs. If `schema` is null then
   * fallback to ast.context().schema()
   */
  public static PlanContext assemblePlan(SqlNode ast, Schema schema) {
    return disambiguateQualification(resolvePlan(buildPlan(ast, schema)));
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
    return recursive ? refs.get(0) : tryResolveRef(ctx, refs.get(0), true);
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

  public static boolean isUniqueCoreAt(PlanContext ctx, Collection<Value> attrs, int surfaceId) {
    if (attrs instanceof Set) return isUniqueCoreAt0(ctx, (Set<Value>) attrs, surfaceId);
    else return isUniqueCoreAt0(ctx, new HashSet<>(attrs), surfaceId);
  }

  public static boolean isNotNullAt(PlanContext ctx, Collection<Value> attrs, int surfaceId) {
    if (attrs instanceof Set) return isNotNullAt0(ctx, (Set<Value>) attrs, surfaceId);
    else return isNotNullAt0(ctx, new HashSet<>(attrs), surfaceId);
  }

  // Must be invoked after `resolvePlan`
  static PlanContext disambiguateQualification(PlanContext ctx) {
    final List<PlanNode> nodes = gatherNodes(ctx, EnumSet.of(Proj, Agg, Input));
    final Set<String> knownQualifications = new HashSet<>(nodes.size());
    final NameSequence seq = NameSequence.mkIndexed("q", 0);

    for (PlanNode node : nodes) {
      assert node instanceof Qualified;
      final Qualified qualified = (Qualified) node;

      if (!mustBeQualified(ctx, ctx.nodeIdOf(node))) continue;
      if (knownQualifications.add(qualified.qualification())) continue;

      final String newQualification = seq.nextUnused(knownQualifications);
      knownQualifications.add(newQualification);

      qualified.setQualification(newQualification);
      for (Value value : ctx.valuesOf(node)) value.setQualification(newQualification);
    }

    return ctx;
  }

  static List<PlanNode> gatherNodes(PlanContext ctx, PlanKind kind) {
    final List<PlanNode> inputs = new ArrayList<>(8);

    for (int i = 0, bound = ctx.maxNodeId(); i < bound; i++)
      if (ctx.isPresent(i) && ctx.kindOf(i) == kind) {
        inputs.add(ctx.nodeAt(i));
      }

    return inputs;
  }

  static List<PlanNode> gatherNodes(PlanContext ctx, EnumSet<PlanKind> kinds) {
    final List<PlanNode> inputs = new ArrayList<>(8);

    for (int i = 0, bound = ctx.maxNodeId(); i < bound; i++)
      if (ctx.isPresent(i) && kinds.contains(ctx.kindOf(i))) {
        inputs.add(ctx.nodeAt(i));
      }

    return inputs;
  }

  //// Expression-related
  public static boolean isColRef(Expression expr) {
    return ColRef.isInstance(expr.template());
  }

  static Expression mkColRefExpr(Value value, SqlContext sqlCtx) {
    return Expression.mk(SqlSupport.mkColRef(sqlCtx, value.qualification(), value.name()));
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

  private static boolean isUniqueCoreAt0(PlanContext ctx, Set<Value> toCheck, int surfaceId) {
    final ValuesRegistry valuesReg = ctx.valuesReg();
    final PlanKind kind = ctx.kindOf(surfaceId);

    if (kind == Input) {
      // Input: check if the attrs are one of the Unique key
      final Set<Column> columns = SetSupport.map(toCheck, valuesReg::columnOf);
      final Table table = ((InputNode) ctx.nodeAt(surfaceId)).table();
      return any(table.constraints(UNIQUE), it -> columns.containsAll(it.columns()));

    } else if (kind == Join) {
      // Join: split the `toCheck` by sides, check if both are the unique key
      final Values lhsValues = valuesReg.valuesOf(ctx.childOf(surfaceId, 0));
      final Set<Value> lhsToCheck = SetSupport.filter(toCheck, lhsValues::contains);
      final Set<Value> rhsToCheck = SetSupport.filter(toCheck, not(lhsToCheck::contains));

      final Expression joinCond = ((JoinNode) ctx.nodeAt(surfaceId)).joinCond();
      if (isEquiJoinCondition(joinCond.template())) {
        // For equi-join, the attrs will be expanded symmetrically.
        // e.g. ["t1.k1"] is the unique-core of Join<t1.k1=t2.k2>(t1,t2)
        // as long as ["t1.k1"] and ["t2.k2"] are the unique-core of t1 and t2, respectively.
        final Values joinKeys = valuesReg.valueRefsOf(joinCond);
        for (int i = 0, bound = joinKeys.size() - 1; i < bound; i += 2) {
          final Value key0 = joinKeys.get(i), key1 = joinKeys.get(i + 1);
          if (lhsToCheck.contains(key0)) rhsToCheck.add(key1);
          else if (rhsToCheck.contains(key0)) lhsToCheck.add(key1);
          if (lhsToCheck.contains(key1)) rhsToCheck.add(key0);
          else if (rhsToCheck.contains(key1)) lhsToCheck.add(key0);
        }
      }

      return isUniqueCoreAt0(ctx, rhsToCheck, ctx.childOf(surfaceId, 1))
          && isUniqueCoreAt0(ctx, lhsToCheck, ctx.childOf(surfaceId, 0));

    } else if (kind == Proj) {
      // Proj: check if the referenced attrs are the unique-core of input of Proj node.
      // e.g. ["n"] is the unique-core of Proj<t.m AS n>(t)
      // as long as ["t.m"] is the unique-core of t
      final ProjNode proj = (ProjNode) ctx.nodeAt(surfaceId);
      if (proj.deduplicated()) return true;

      final Set<Value> refAttrs = new HashSet<>(toCheck.size());
      for (Value value : toCheck) {
        final Value ref = tryResolveRef(ctx, value);
        if (ref != null) refAttrs.add(ref);
      }
      return isUniqueCoreAt0(ctx, refAttrs, ctx.childOf(surfaceId, 0));

    } else if (kind == Filter) {
      // Filter: check if the attrs are the unique core of Filter's input
      final Expression predicate = ((SimpleFilterNode) ctx.nodeAt(surfaceId)).predicate();
      if (isEquiConstCondition(predicate.template())) {
        // If the filter is in form of "col_ref = const_value", then add "col_ref" to `to_check`
        // e.g. [] is the unique core of Filter<t.a = 1>(t)
        // as long as ["t.a"] is the unique core of t.
        final Values refs = valuesReg.valueRefsOf(predicate);
        assert refs.size() == 1;
        toCheck.add(refs.get(0));
      }
      return isUniqueCoreAt0(ctx, toCheck, ctx.childOf(surfaceId, 0));

    } else if (kind == Agg) {
      // Agg: check if all group keys are contained by `toCheck`
      // e.g., ["t.a","t.b"] is the unique core of Agg<group=["t.a"]>(t)
      final List<Expression> groupExprs = ((AggNode) ctx.nodeAt(surfaceId)).groupByExprs();
      final Set<Value> groupAttrs = SetSupport.flatMap(groupExprs, valuesReg::valueRefsOf);

      for (Value value : toCheck) {
        final Value ref = tryResolveRef(ctx, value);
        if (ref == null) return false;
        groupAttrs.remove(ref);
      }
      return groupAttrs.isEmpty();

    } else {
      return isUniqueCoreAt0(ctx, toCheck, ctx.childOf(surfaceId, 0));
    }
  }

  private static boolean isNotNullAt0(PlanContext ctx, Set<Value> toCheck, int surfaceId) {
    if (toCheck.isEmpty()) return true;

    final PlanKind kind = ctx.kindOf(surfaceId);
    final ValuesRegistry valuesReg = ctx.valuesReg();
    if (kind == Input) {
      // Input: check if the attrs are one of the NOT_NULL keys
      final Set<Column> columns = SetSupport.map(toCheck, valuesReg::columnOf);
      final Table table = ((InputNode) ctx.nodeAt(surfaceId)).table();
      return any(table.constraints(NOT_NULL), it -> columns.containsAll(it.columns()));

    } else if (kind == Join) {
      // Join: split the `toCheck` by sides, check if both are NOT_NULL
      final JoinNode join = (JoinNode) ctx.nodeAt(surfaceId);
      final Values lhsValues = valuesReg.valuesOf(ctx.childOf(surfaceId, 0));
      final Set<Value> lhsToCheck = SetSupport.filter(toCheck, lhsValues::contains);
      final Set<Value> rhsToCheck = SetSupport.filter(toCheck, not(lhsToCheck::contains));
      if (join.joinKind() == JoinKind.INNER_JOIN) {
        // For inner-join, join keys are removed
        final Values joinKeys = valuesReg.valueRefsOf(join.joinCond());
        joinKeys.forEach(lhsToCheck::remove);
        joinKeys.forEach(rhsToCheck::remove);
      }

      return isUniqueCoreAt0(ctx, rhsToCheck, ctx.childOf(surfaceId, 1))
          && isUniqueCoreAt0(ctx, lhsToCheck, ctx.childOf(surfaceId, 0));

    } else if (kind == Filter) {
      // Filter: check if the attrs are NOT_NULL at Filter's input
      final Expression predicate = ((SimpleFilterNode) ctx.nodeAt(surfaceId)).predicate();
      if (isEquiConstCondition(predicate.template())) {
        // If the filter is of "col_ref = const_value", then remove "col_ref" from `to_check`
        final Values refs = valuesReg.valueRefsOf(predicate);
        assert refs.size() == 1;
        toCheck.remove(refs.get(0));
      }
      return isUniqueCoreAt0(ctx, toCheck, ctx.childOf(surfaceId, 0));

    } else {
      return isUniqueCoreAt(ctx, toCheck, ctx.childOf(surfaceId, 0));
    }
  }
}
