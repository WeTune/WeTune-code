package sjtu.ipads.wtune.superopt.profiler;

import com.google.common.collect.Iterables;
import org.apache.commons.lang3.tuple.Pair;
import sjtu.ipads.wtune.sqlparser.ast.ASTNode;
import sjtu.ipads.wtune.sqlparser.ast.constants.LiteralType;
import sjtu.ipads.wtune.sqlparser.schema.Column;
import sjtu.ipads.wtune.sqlparser.schema.Schema;
import sjtu.ipads.wtune.stmt.resolver.ParamDesc;
import sjtu.ipads.wtune.stmt.resolver.ParamModifier;
import sjtu.ipads.wtune.stmt.resolver.ParamModifier.Type;
import sjtu.ipads.wtune.stmt.resolver.Params;
import sjtu.ipads.wtune.stmt.resolver.Resolution;
import sjtu.ipads.wtune.superopt.util.Complexity;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import static sjtu.ipads.wtune.sqlparser.ast.ExprFields.LITERAL_TYPE;
import static sjtu.ipads.wtune.sqlparser.ast.ExprFields.LITERAL_VALUE;
import static sjtu.ipads.wtune.sqlparser.ast.constants.ExprKind.LITERAL;
import static sjtu.ipads.wtune.sqlparser.ast.constants.ExprKind.PARAM_MARKER;
import static sjtu.ipads.wtune.sqlparser.plan.PlanSupport.buildPlan;

public interface ProfilerSupport {
  static Pair<ASTNode, double[]> pickMinCost(
      Schema schema, ASTNode baseline, Iterable<ASTNode> candidates, Properties dbProps) {
    final ASTNode candidate0 = Iterables.getFirst(candidates, null);
    if (candidate0 == null) return null;

    // MySQL doesn't correctly estimate some simplification (e.g. remove JOIN),
    // so let's do it ourself.

    final Complexity complex0 = Complexity.mk(buildPlan(candidate0, schema));
    final Complexity complex1 = Complexity.mk(buildPlan(baseline, schema));
    final int comparison = complex0.compareTo(complex1, false);
    if (comparison < 0) return Pair.of(candidate0, new double[] {-1, -2});
    assert comparison == 0;

    final double baseCost = getCost(baseline, dbProps);
    if (baseCost == Double.MAX_VALUE) return null;

    double minCost = baseCost;
    ASTNode minCostCandidate = null;
    for (ASTNode candidate : candidates) {
      final double cost = getCost(candidate, dbProps);
      if (cost >= minCost) continue;
      minCost = cost;
      minCostCandidate = candidate;
    }

    if (minCostCandidate == null) return null;
    else return Pair.of(minCostCandidate, new double[] {baseCost, minCost});
  }

  static double getCost(ASTNode ast, Properties dbProps) {
    final List<ASTNode> filled = fillParamMarker(ast);
    final String query = ast.toString();
    unFillParamMarker(filled);

    final DataSource dataSource = DataSourceFactory.instance().make(dbProps);
    final String dbType = dbProps.getProperty("dbType");

    return CostQuery.make(dbType, dataSource::getConnection, query).getCost();
  }

  static List<ASTNode> fillParamMarker(ASTNode ast) {
    final Params mgr = Resolution.resolveParamFull(ast);
    final List<ASTNode> filled = new ArrayList<>();
    for (ParamDesc param : mgr.params()) {
      final ASTNode node = param.node();
      if (!PARAM_MARKER.isInstance(node)) continue;

      final ParamModifier modifier = param.modifiers().getLast();
      if (modifier == null || modifier.type() != Type.COLUMN_VALUE) continue;

      final Schema schema = ast.context().schema();
      final Column column =
          schema.table((String) modifier.args()[0]).column((String) modifier.args()[1]);
      assert column != null;

      final ASTNode value = ASTNode.expr(LITERAL);
      switch (column.dataType().category()) {
        case INTEGRAL:
          value.set(LITERAL_TYPE, LiteralType.INTEGER);
          value.set(LITERAL_VALUE, 1);
          break;
        case FRACTION:
          value.set(LITERAL_TYPE, LiteralType.FRACTIONAL);
          value.set(LITERAL_VALUE, 1.0);
          break;
        case BOOLEAN:
          value.set(LITERAL_TYPE, LiteralType.BOOL);
          value.set(LITERAL_VALUE, false);
          break;
        case STRING:
          value.set(LITERAL_TYPE, LiteralType.TEXT);
          value.set(LITERAL_VALUE, "00001");
          break;
        case TIME:
          value.set(LITERAL_TYPE, LiteralType.TEXT);
          value.set(LITERAL_VALUE, "2021-01-01 00:00:00.000");
          break;
        default:
          value.set(LITERAL_TYPE, LiteralType.NULL);
      }
      node.update(value);
      filled.add(node);
    }
    return filled;
  }

  static void unFillParamMarker(List<ASTNode> filled) {
    for (ASTNode n : filled) {
      final ASTNode marker = ASTNode.expr(PARAM_MARKER);
      n.update(marker);
    }
  }
}
