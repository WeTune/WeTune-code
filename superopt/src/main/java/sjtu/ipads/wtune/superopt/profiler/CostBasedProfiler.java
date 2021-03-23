package sjtu.ipads.wtune.superopt.profiler;

import static sjtu.ipads.wtune.common.utils.Commons.tail;
import static sjtu.ipads.wtune.common.utils.FuncUtils.listFlatMap;
import static sjtu.ipads.wtune.sqlparser.ast.ExprFields.LITERAL_TYPE;
import static sjtu.ipads.wtune.sqlparser.ast.ExprFields.LITERAL_VALUE;
import static sjtu.ipads.wtune.sqlparser.ast.constants.ExprKind.LITERAL;
import static sjtu.ipads.wtune.sqlparser.ast.constants.ExprKind.PARAM_MARKER;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import javax.sql.DataSource;
import sjtu.ipads.wtune.sqlparser.ast.ASTNode;
import sjtu.ipads.wtune.sqlparser.ast.constants.LiteralType;
import sjtu.ipads.wtune.sqlparser.schema.Column;
import sjtu.ipads.wtune.sqlparser.schema.Schema;
import sjtu.ipads.wtune.stmt.resolver.Param;
import sjtu.ipads.wtune.stmt.resolver.ParamManager;
import sjtu.ipads.wtune.stmt.resolver.ParamModifier;
import sjtu.ipads.wtune.stmt.resolver.ParamModifier.Type;
import sjtu.ipads.wtune.stmt.resolver.Resolution;

public class CostBasedProfiler implements Profiler {
  private final Properties connProps;
  private final DataSource dataSource;

  public CostBasedProfiler(Properties connProps) {
    this.connProps = connProps;
    this.dataSource = DataSourceFactory.instance().make(connProps);
  }

  @Override
  public List<ASTNode> pickOptimized(ASTNode baseline, List<ASTNode> candidates) {
    final List<ASTNode> filled0 = fillParamMarker(baseline);

    final double baselineCost = getCost(baseline.toString());
    if (baselineCost == Double.MAX_VALUE) return Collections.emptyList();

    double minCost = baselineCost;
    final List<ASTNode> optimized = new ArrayList<>();

    final List<ASTNode> filled1 = listFlatMap(CostBasedProfiler::fillParamMarker, candidates);

    for (ASTNode query : candidates) {
      final double cost = getCost(query.toString());
      if (cost > minCost) continue;
      if (cost < minCost) optimized.clear();
      minCost = cost;
      optimized.add(query);
    }

    unFillParamMarker(filled0);
    unFillParamMarker(filled1);

    System.out.println(minCost + " " + baselineCost);
    if (minCost <= baselineCost * 0.9) return optimized;
    else return Collections.emptyList();
  }

  private static List<ASTNode> fillParamMarker(ASTNode ast) {
    final ParamManager mgr = Resolution.resolveParamFull(ast);
    final List<ASTNode> filled = new ArrayList<>();
    for (Param param : mgr.params()) {
      final ASTNode node = param.node();
      if (!PARAM_MARKER.isInstance(node)) continue;

      final ParamModifier modifier = tail(param.modifiers());
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

  private static void unFillParamMarker(List<ASTNode> filled) {
    for (ASTNode n : filled) {
      final ASTNode marker = ASTNode.expr(PARAM_MARKER);
      n.update(marker);
    }
  }

  private double getCost(String query) {
    return CostQuery.of(connProps.getProperty("dbType"), dataSource::getConnection, query)
        .getCost();
  }
}
