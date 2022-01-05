package sjtu.ipads.wtune.sql.support.resolution;

import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.procedure.TObjectProcedure;
import sjtu.ipads.wtune.sql.ast.SqlContext;
import sjtu.ipads.wtune.sql.ast.SqlNode;

import java.util.function.Consumer;
import java.util.function.Predicate;

import static java.util.Collections.singletonList;
import static sjtu.ipads.wtune.sql.ast.SqlNodeFields.Query_Limit;
import static sjtu.ipads.wtune.sql.ast.SqlNodeFields.Query_Offset;
import static sjtu.ipads.wtune.sql.support.locator.LocatorSupport.*;
import static sjtu.ipads.wtune.sql.support.resolution.JoinGraph.JOIN_GRAPH;
import static sjtu.ipads.wtune.sql.support.resolution.ParamModifier.Type.LIMIT_VAL;
import static sjtu.ipads.wtune.sql.support.resolution.ParamModifier.Type.OFFSET_VAL;
import static sjtu.ipads.wtune.sql.support.resolution.ParamModifier.modifier;
import static sjtu.ipads.wtune.sql.support.resolution.ResolutionSupport.*;

class ParamsImpl implements Params {
  private final SqlContext ctx;
  private TIntObjectMap<ParamDesc> params;

  ParamsImpl(SqlContext ctx) {
    this.ctx = ctx;
  }

  private TIntObjectMap<ParamDesc> params() {
    if (params == null) {
      final TIntObjectMap<ParamDesc> params = new TIntObjectHashMap<>();
      final SqlNode rootNode = SqlNode.mk(ctx, ctx.root());

      if (limitClauseAsParam) {
        for (SqlNode limitNode : clauseLocator().accept(Query_Limit).gather(rootNode))
          params.put(limitNode.nodeId(), mkLimitParam(limitNode));

        for (SqlNode offsetNode : clauseLocator().accept(Query_Offset).gather(rootNode))
          params.put(offsetNode.nodeId(), mkOffsetParam(offsetNode));
      }

      for (SqlNode predicate : predicateLocator().primitive().gather(rootNode))
        for (ParamDesc paramDesc : new ResolveParam().resolve(predicate))
          if (paramDesc != null) params.put(paramDesc.node().nodeId(), paramDesc);

      int index = 0;
      for (SqlNode paramNode : nodeLocator().accept(it -> paramOf(it) != null).gather(rootNode)) {
        final ParamDesc param = paramOf(paramNode);
        if (!isCheckNull(param)) param.setIndex(index++);
        if (isElementParam(param)) ++index;
      }

      this.params = params;
    }

    return params;
  }

  @Override
  public int numParams() {
    return params().size();
  }

  @Override
  public ParamDesc paramOf(SqlNode node) {
    return params().get(node.nodeId());
  }

  @Override
  public void forEach(Consumer<ParamDesc> consumer) {
    params.forEachValue(
        param -> {
          consumer.accept(param);
          return true;
        });
  }

  @Override
  public boolean forEach(Predicate<ParamDesc> consumer) {
    final ParamVisitor visitor = new ParamVisitor(consumer);
    params.forEachValue(visitor);
    return visitor.result;
  }

  @Override
  public void relocateNode(int oldId, int newId) {
    if (params == null) return;
    final ParamDesc param = params.get(oldId);
    if (param != null) params().put(newId, param);
  }

  @Override
  public void deleteNode(int nodeId) {
    if (params == null) return;
    params.remove(nodeId);
  }

  @Override
  public JoinGraph joinGraph() {
    return ctx.getAdditionalInfo(JOIN_GRAPH);
  }

  private static ParamDesc mkOffsetParam(SqlNode offsetNode) {
    return new ParamDescImpl(null, offsetNode, singletonList(modifier(OFFSET_VAL)));
  }

  private static ParamDesc mkLimitParam(SqlNode paramNode) {
    return new ParamDescImpl(null, paramNode, singletonList(modifier(LIMIT_VAL)));
  }

  private static class ParamVisitor implements TObjectProcedure<ParamDesc> {
    private boolean result;
    private final Predicate<ParamDesc> action;

    private ParamVisitor(Predicate<ParamDesc> action) {
      this.result = true;
      this.action = action;
    }

    @Override
    public boolean execute(ParamDesc object) {
      if (!action.test(object)) {
        result = false;
        return false;
      }
      return true;
    }
  }
}
