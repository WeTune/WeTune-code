package sjtu.ipads.wtune.sql.resolution;

import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import sjtu.ipads.wtune.sql.ast1.SqlContext;
import sjtu.ipads.wtune.sql.ast1.SqlNode;
import sjtu.ipads.wtune.sql.ast1.SqlVisitor;

import java.util.List;

import static java.util.Collections.singletonList;
import static sjtu.ipads.wtune.common.utils.Commons.tail;
import static sjtu.ipads.wtune.common.utils.IterableSupport.any;
import static sjtu.ipads.wtune.sql.SqlSupport.isPrimitivePredicate;
import static sjtu.ipads.wtune.sql.ast1.ExprFields.Binary_Op;
import static sjtu.ipads.wtune.sql.ast1.ExprKind.Exists;
import static sjtu.ipads.wtune.sql.ast1.SqlKind.Query;
import static sjtu.ipads.wtune.sql.ast1.SqlNodeFields.Query_Limit;
import static sjtu.ipads.wtune.sql.ast1.SqlNodeFields.Query_Offset;
import static sjtu.ipads.wtune.sql.ast1.constants.BinaryOpKind.IN_SUBQUERY;
import static sjtu.ipads.wtune.sql.resolution.ParamModifier.Type.*;
import static sjtu.ipads.wtune.sql.resolution.ParamModifier.modifier;

class ParamsImpl implements Params {
  private final TIntObjectMap<ParamDesc> params;

  ParamsImpl(SqlContext ctx) {
    params = new TIntObjectHashMap<>();
    SqlNode.mk(ctx, ctx.root()).accept(new ExtractParams());
  }

  @Override
  public ParamDesc paramOf(SqlNode node) {
    return params.get(node.nodeId());
  }

  @Override
  public void renumberNode(int oldId, int newId) {
    final ParamDesc param = params.get(oldId);
    if (param != null) params.put(newId, param);
  }

  @Override
  public void deleteNode(int nodeId) {
    params.remove(nodeId);
  }

  private class ExtractParams implements SqlVisitor {
    private int nextIndex = 0;

    @Override
    public boolean enter(SqlNode node) {
      if (Query.isInstance(node)) {
        final SqlNode offset = node.$(Query_Offset);
        final SqlNode limit = node.$(Query_Limit);
        if (offset != null) {
          final ParamDesc desc =
              new ParamDescImpl(null, offset, singletonList(modifier(OFFSET_VAL)));
          params.put(desc.node().nodeId(), desc);
        }

        if (limit != null) {
          final ParamDesc desc = new ParamDescImpl(null, limit, singletonList(modifier(LIMIT_VAL)));
          params.put(desc.node().nodeId(), desc);
        }
      }

      if (!isPrimitivePredicate(node)) return true;

      final List<ParamDesc> paramDescs = ResolveParam.resolve(node);
      if (paramDescs.contains(null)) return false;

      for (ParamDesc desc : paramDescs) {
        if (!isCheckNull(desc)) desc.setIndex(nextIndex++);
        if (isElement(desc)) nextIndex++;
        params.put(desc.node().nodeId(), desc);
      }

      return node.get(Binary_Op) == IN_SUBQUERY || Exists.isInstance(node);
    }

    private static boolean isCheckNull(ParamDesc desc) {
      final ParamModifier.Type lastModifierType = tail(desc.modifiers()).type();
      return lastModifierType == CHECK_NULL || lastModifierType == CHECK_NULL_NOT;
    }

    private static boolean isElement(ParamDesc desc) {
      return any(desc.modifiers(), it -> it.type() == ARRAY_ELEMENT || it.type() == TUPLE_ELEMENT);
    }
  }
}
