package sjtu.ipads.wtune.sql.plan;

import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.procedure.TIntObjectProcedure;
import org.apache.commons.lang3.tuple.Pair;
import sjtu.ipads.wtune.common.utils.COW;
import sjtu.ipads.wtune.sql.ast.constants.JoinKind;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import static sjtu.ipads.wtune.common.tree.TreeContext.NO_SUCH_NODE;

public class InfoCacheImpl implements InfoCache {
  private final COW<TIntObjectMap<Pair<List<Value>, List<Value>>>> joinKeys;
  private final COW<TIntObjectMap<JoinKind>> joinKinds;
  private final COW<TIntObjectMap<Expression>> subqueryExprs;
  private final COW<Map<Expression, int[]>> virtualExprs;

  InfoCacheImpl() {
    this.joinKeys = new COW<>(new TIntObjectHashMap<>(), null);
    this.joinKinds = new COW<>(new TIntObjectHashMap<>(), null);
    this.subqueryExprs = new COW<>(new TIntObjectHashMap<>(), null);
    this.virtualExprs = new COW<>(new IdentityHashMap<>(), null);
  }

  InfoCacheImpl(InfoCacheImpl toCopy) {
    this.joinKeys = new COW<>(toCopy.joinKeys.forRead(), TIntObjectHashMap::new);
    this.joinKinds = new COW<>(toCopy.joinKinds.forRead(), TIntObjectHashMap::new);
    this.subqueryExprs = new COW<>(toCopy.subqueryExprs.forRead(), TIntObjectHashMap::new);
    this.virtualExprs = new COW<>(toCopy.virtualExprs.forRead(), HashMap::new);
  }

  @Override
  public void putJoinKeyOf(int joinNodeId, List<Value> lhsKeys, List<Value> rhsKeys) {
    joinKeys.forWrite().put(joinNodeId, Pair.of(lhsKeys, rhsKeys));
  }

  @Override
  public void putJoinKindOf(int joinNodeId, JoinKind joinKind) {
    joinKinds.forWrite().put(joinNodeId, joinKind);
  }

  @Override
  public void putSubqueryExprOf(int inSubNodeId, Expression expr) {
    subqueryExprs.forWrite().put(inSubNodeId, expr);
  }

  @Override
  public void putVirtualExpr(Expression compoundExpr, int... nodes) {
    virtualExprs.forWrite().put(compoundExpr, nodes);
  }

  @Override
  public Pair<List<Value>, List<Value>> getJoinKeyOf(int nodeId) {
    return joinKeys.forRead().get(nodeId);
  }

  @Override
  public JoinKind getJoinKindOf(int nodeId) {
    return joinKinds.forRead().get(nodeId);
  }

  @Override
  public Expression getSubqueryExprOf(int nodeId) {
    return subqueryExprs.forRead().get(nodeId);
  }

  @Override
  public int getSubqueryNodeOf(Expression expr) {
    final SubqueryNodeFinder finder = new SubqueryNodeFinder(expr);
    subqueryExprs.forRead().forEachEntry(finder);
    return finder.subqueryNode;
  }

  @Override
  public int[] getVirtualExprComponents(Expression expr) {
    return virtualExprs.forRead().get(expr);
  }

  void deleteNode(int nodeId) {
    if (joinKeys.forRead().containsKey(nodeId)) joinKeys.forWrite().remove(nodeId);
    if (joinKinds.forRead().containsKey(nodeId)) joinKinds.forWrite().remove(nodeId);
    if (subqueryExprs.forRead().containsKey(nodeId)) subqueryExprs.forWrite().remove(nodeId);
  }

  void renumberNode(int from, int to) {
    final var keys = joinKeys.forRead().get(from);
    if (keys != null) {
      joinKeys.forWrite().put(to, keys);
      joinKeys.forWrite().remove(from);
    }

    final JoinKind kind = joinKinds.forRead().get(from);
    if (kind != null) {
      joinKinds.forWrite().put(to, kind);
      joinKeys.forWrite().remove(from);
    }

    final Expression subqueryExpr = subqueryExprs.forRead().get(from);
    if (subqueryExpr != null) {
      subqueryExprs.forWrite().put(to, subqueryExpr);
      subqueryExprs.forWrite().remove(from);
    }
  }

  void clearVirtualExprs() {
    virtualExprs.forWrite().clear();
  }

  private static class SubqueryNodeFinder implements TIntObjectProcedure<Expression> {
    private int subqueryNode = NO_SUCH_NODE;
    private final Expression target;

    private SubqueryNodeFinder(Expression target) {
      this.target = target;
    }

    @Override
    public boolean execute(int a, Expression b) {
      if (b == target) {
        subqueryNode = a;
        return false;
      }
      return true;
    }
  }
}
