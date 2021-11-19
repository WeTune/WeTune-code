package sjtu.ipads.wtune.sqlparser.plan1;

import sjtu.ipads.wtune.common.utils.NameSequence;
import sjtu.ipads.wtune.sqlparser.ast1.SqlContext;
import sjtu.ipads.wtune.sqlparser.ast1.SqlNode;
import sjtu.ipads.wtune.sqlparser.ast1.constants.BinaryOpKind;
import sjtu.ipads.wtune.sqlparser.ast1.constants.LiteralKind;
import sjtu.ipads.wtune.sqlparser.schema.Schema;

import java.util.*;

import static sjtu.ipads.wtune.sqlparser.SqlSupport.copyAst;
import static sjtu.ipads.wtune.sqlparser.ast1.ExprFields.*;
import static sjtu.ipads.wtune.sqlparser.ast1.ExprKind.*;
import static sjtu.ipads.wtune.sqlparser.ast1.SqlKind.ColName;
import static sjtu.ipads.wtune.sqlparser.ast1.SqlKind.Expr;
import static sjtu.ipads.wtune.sqlparser.ast1.SqlNodeFields.*;
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
  static Expression mkColRefExpr(Value value, SqlContext sqlCtx) {
    final SqlNode colName = SqlNode.mk(sqlCtx, sqlCtx.mkNode(ColName));
    final SqlNode colRef = SqlNode.mk(sqlCtx, sqlCtx.mkNode(Expr));
    colName.$(ColName_Table, value.qualification());
    colName.$(ColName_Col, value.name());
    colRef.$(Expr_Kind, ColRef);
    colRef.$(ColRef_ColName, colName);
    return Expression.mk(colRef);
  }

  static SqlNode normalizePredicate(SqlNode exprAst, SqlContext sqlCtx) {
    if (ColRef.isInstance(exprAst)) {
      final SqlNode literal = SqlNode.mk(sqlCtx, Expr);
      final SqlNode binary = SqlNode.mk(sqlCtx, Expr);
      literal.$(Expr_Kind, Literal);
      literal.$(Literal_Kind, LiteralKind.BOOL);
      literal.$(Literal_Value, Boolean.TRUE);
      binary.$(Expr_Kind, Binary);
      binary.$(Binary_Left, copyAst(exprAst, sqlCtx));
      binary.$(Binary_Op, BinaryOpKind.IS);
      binary.$(Binary_Right, literal);
      return binary;
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
