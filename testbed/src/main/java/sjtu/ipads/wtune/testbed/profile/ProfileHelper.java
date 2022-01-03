package sjtu.ipads.wtune.testbed.profile;

import org.apache.commons.lang3.tuple.Pair;
import sjtu.ipads.wtune.sql.ast.ASTNode;
import sjtu.ipads.wtune.sql.ast.constants.LiteralType;
import sjtu.ipads.wtune.stmt.Statement;
import sjtu.ipads.wtune.stmt.resolver.ParamDesc;
import sjtu.ipads.wtune.stmt.resolver.ParamModifier;
import sjtu.ipads.wtune.stmt.resolver.ParamModifier.Type;
import sjtu.ipads.wtune.stmt.resolver.Params;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;

import static sjtu.ipads.wtune.common.utils.Commons.tail;
import static sjtu.ipads.wtune.sql.ast.ASTVistor.topDownVisit;
import static sjtu.ipads.wtune.sql.ast.ExprFields.*;
import static sjtu.ipads.wtune.sql.ast.NodeFields.QUERY_LIMIT;
import static sjtu.ipads.wtune.sql.ast.NodeFields.QUERY_OFFSET;
import static sjtu.ipads.wtune.sql.ast.constants.ExprKind.*;
import static sjtu.ipads.wtune.sql.ast.constants.NodeType.QUERY;
import static sjtu.ipads.wtune.stmt.resolver.Resolution.resolveParamFull;
import static sjtu.ipads.wtune.stmt.support.Workflow.normalize;

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
    params.params().removeIf(ProfileHelper::isLimitParam);
    params.params().forEach(ProfileHelper::installParamMarker);

    return params;
  }

  private static void installParamMarker(ParamDesc desc) {
    final Type lastModifierType = tail(desc.modifiers()).type();
    if (lastModifierType == Type.CHECK_NULL) {
      desc.node().set(LITERAL_TYPE, LiteralType.NOT_NULL);
      return;
    }
    if (lastModifierType == Type.CHECK_NULL_NOT) {
      desc.node().set(LITERAL_TYPE, LiteralType.NULL);
      return;
    }
    if (desc.isElement()) {
      final ASTNode tuple = desc.node().parent();
      assert TUPLE.isInstance(tuple);
      final ASTNode paramMarker = ASTNode.expr(PARAM_MARKER);
      paramMarker.set(PARAM_MARKER_FORCE_QUESTION, true);

      final List<ASTNode> paramMarkers = new ArrayList<>(2);
      paramMarkers.add(paramMarker);
      paramMarkers.add(paramMarker.deepCopy());

      tuple.set(TUPLE_EXPRS, paramMarkers);
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
      literal.set(LITERAL_VALUE, 1000000);
      limitClause.update(literal);
    }

    final ASTNode offsetClause = node.get(QUERY_OFFSET);
    if (PARAM_MARKER.isInstance(offsetClause)) {
      final ASTNode literal = ASTNode.expr(LITERAL);
      literal.set(LITERAL_TYPE, LiteralType.INTEGER);
      literal.set(LITERAL_VALUE, 0);
      offsetClause.update(literal);
    }
  }

  private static boolean isLimitParam(ParamDesc desc) {
    final ParamModifier mod = tail(desc.modifiers());
    return mod != null && (mod.type() == Type.OFFSET_VAL || mod.type() == Type.LIMIT_VAL);
  }

  static Pair<Metric, Metric> compare(Statement stmt0, Statement stmt1, ProfileConfig config) {
    setupParams(stmt0);
    setupParams(stmt1);

    final Profiler profiler0 = Profiler.make(stmt0, config);
    final Profiler profiler1 = Profiler.make(stmt1, config);

    if (!tryReadParams(profiler0, config) || !tryReadParams(profiler1, config)) {
      ParamsGen.alignTables(profiler0.paramsGen(), profiler1.paramsGen());

      if (!profiler0.prepare()) return null;
      profiler1.setSeeds(profiler0.seeds());
      if (!profiler1.prepare()) return null;

      trySaveParams(profiler0, config);
      trySaveParams(profiler1, config);
    }

    System.out.println(stmt0 + ".base ");
    if (!profiler0.run()) return null;
    System.out.println(stmt0 + ".opt ");
    if (!profiler1.run()) return null;

    profiler0.close();
    profiler1.close();
    config.executorFactory().close();

    return Pair.of(profiler0.metric(), profiler1.metric());
  }

  private static boolean tryReadParams(Profiler profiler, ProfileConfig config) {
    final ObjectInputStream stream = config.saveParamIn(profiler.statement());
    if (stream == null) return false;
    try (stream) {
      return profiler.readParams(stream);
    } catch (IOException | ClassNotFoundException e) {
      return false;
    }
  }

  private static void trySaveParams(Profiler profiler, ProfileConfig config) {
    final ObjectOutputStream stream = config.savedParamOut(profiler.statement());
    if (stream == null) return;
    try (stream) {
      profiler.saveParams(stream);
    } catch (IOException ignored) {
    }
  }
}
