package sjtu.ipads.wtune.sql.support.resolution;

import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import sjtu.ipads.wtune.sql.ast1.SqlContext;
import sjtu.ipads.wtune.sql.ast1.SqlNode;

import static java.util.Collections.singletonList;
import static sjtu.ipads.wtune.sql.ast1.SqlNodeFields.Query_Limit;
import static sjtu.ipads.wtune.sql.ast1.SqlNodeFields.Query_Offset;
import static sjtu.ipads.wtune.sql.support.locator.LocatorSupport.clauseLocator;
import static sjtu.ipads.wtune.sql.support.locator.LocatorSupport.predicateLocator;
import static sjtu.ipads.wtune.sql.support.resolution.ParamModifier.Type.LIMIT_VAL;
import static sjtu.ipads.wtune.sql.support.resolution.ParamModifier.Type.OFFSET_VAL;
import static sjtu.ipads.wtune.sql.support.resolution.ParamModifier.modifier;

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

      for (SqlNode limitNode : clauseLocator().accept(Query_Limit).gather(rootNode))
        params.put(limitNode.nodeId(), mkLimitParam(limitNode));

      for (SqlNode offsetNode : clauseLocator().accept(Query_Offset).gather(rootNode))
        params.put(offsetNode.nodeId(), mkOffsetParam(offsetNode));

      for (SqlNode predicate : predicateLocator().primitive().gather(rootNode))
        for (ParamDesc paramDesc : new ResolveParam().resolve(predicate))
          if (paramDesc != null) params.put(paramDesc.node().nodeId(), paramDesc);

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

  private static ParamDesc mkOffsetParam(SqlNode offsetNode) {
    return new ParamDescImpl(null, offsetNode, singletonList(modifier(OFFSET_VAL)));
  }

  private static ParamDesc mkLimitParam(SqlNode paramNode) {
    return new ParamDescImpl(null, paramNode, singletonList(modifier(LIMIT_VAL)));
  }
}
