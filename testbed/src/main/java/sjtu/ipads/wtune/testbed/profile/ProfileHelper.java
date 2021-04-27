package sjtu.ipads.wtune.testbed.profile;

import static sjtu.ipads.wtune.sqlparser.ast.ASTVistor.topDownVisit;
import static sjtu.ipads.wtune.sqlparser.ast.ExprFields.LITERAL_TYPE;
import static sjtu.ipads.wtune.sqlparser.ast.ExprFields.LITERAL_VALUE;
import static sjtu.ipads.wtune.sqlparser.ast.ExprFields.PARAM_MARKER_FORCE_QUESTION;
import static sjtu.ipads.wtune.sqlparser.ast.NodeFields.QUERY_LIMIT;
import static sjtu.ipads.wtune.sqlparser.ast.constants.ExprKind.LITERAL;
import static sjtu.ipads.wtune.sqlparser.ast.constants.ExprKind.PARAM_MARKER;
import static sjtu.ipads.wtune.sqlparser.ast.constants.NodeType.QUERY;
import static sjtu.ipads.wtune.stmt.resolver.Resolution.resolveParamFull;
import static sjtu.ipads.wtune.stmt.support.Workflow.normalize;

import org.apache.commons.lang3.tuple.Pair;
import sjtu.ipads.wtune.sqlparser.ast.ASTNode;
import sjtu.ipads.wtune.sqlparser.ast.constants.LiteralType;
import sjtu.ipads.wtune.stmt.Statement;
import sjtu.ipads.wtune.stmt.resolver.ParamDesc;
import sjtu.ipads.wtune.stmt.resolver.ParamModifier.Type;
import sjtu.ipads.wtune.stmt.resolver.Params;

public interface ProfileHelper {
  /**
   * Setup necessary context for a statement to be profiled, including:
   *
   * <ol>
   *   <li>setup schema
   *   <li>normalize the sql
   *   <li>resolve parameters
   *   <li>replace all parameters by ParamMarker
   * </ol>
   */
  static Params setupParams(Statement stmt) {
    final ASTNode ast = stmt.parsed();
    ast.context().setSchema(stmt.app().schema("base", true));
    ast.accept(topDownVisit(ProfileHelper::setupLIMIT, QUERY));
    normalize(ast);

    final Params params = resolveParamFull(ast);
    params.params().forEach(ProfileHelper::installParamMarker);

    return params;
  }

  private static void installParamMarker(ParamDesc desc) {
    final Type lastModifierType = desc.modifiers().getLast().type();
    if (lastModifierType == Type.CHECK_NULL) {
      desc.node().set(LITERAL_TYPE, LiteralType.NOT_NULL);
      return;
    }
    if (lastModifierType == Type.CHECK_NULL_NOT) {
      desc.node().set(LITERAL_TYPE, LiteralType.NULL);
      return;
    }

    final ASTNode paramMarker = ASTNode.expr(PARAM_MARKER);
    paramMarker.set(PARAM_MARKER_FORCE_QUESTION, true);

    desc.node().update(paramMarker);
  }

  private static void setupLIMIT(ASTNode node) {
    final ASTNode limitClause = node.get(QUERY_LIMIT);
    if (PARAM_MARKER.isInstance(limitClause)) {
      final ASTNode literal = ASTNode.expr(LITERAL);
      literal.set(LITERAL_TYPE, LiteralType.INTEGER);
      literal.set(LITERAL_VALUE, 100);
      limitClause.update(literal);
    }
  }

  static Pair<Metric, Metric> compare(Statement stmt0, Statement stmt1, ProfileConfig config) {
    setupParams(stmt0);
    setupParams(stmt1);

    final Profiler profiler0 = Profiler.make(stmt0, config);
    final Profiler profiler1 = Profiler.make(stmt1, config);

    ParamsGen.alignTables(profiler0.paramsGen(), profiler1.paramsGen());

    if (!profiler0.prepare()) return null;
    profiler1.setSeeds(profiler0.seeds());
    if (!profiler1.prepare()) return null;

    if (!profiler0.run()) return null;
    if (!profiler1.run()) return null;

    profiler0.close();
    profiler1.close();
    config.executorFactory().close();

    return Pair.of(profiler0.metric(), profiler1.metric());
  }
}
