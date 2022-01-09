package sjtu.ipads.wtune.sql.plan;

import org.apache.commons.lang3.tuple.Pair;
import sjtu.ipads.wtune.sql.ast.constants.JoinKind;

import java.util.List;

public interface InfoCache {
  int getSubqueryNodeOf(Expression expr);

  int[] getVirtualExprComponents(Expression expr);

  JoinKind getJoinKindOf(int nodeId);

  Expression getSubqueryExprOf(int nodeId);

  Pair<List<Value>, List<Value>> getJoinKeyOf(int nodeId);

  void putJoinKeyOf(int joinNodeId, List<Value> lhsKeys, List<Value> rhsKeys);

  void putJoinKindOf(int joinNodeId, JoinKind joinKind);

  void putSubqueryExprOf(int inSubNodeId, Expression expr);

  void putVirtualExpr(Expression compoundExpr, int... nodes);

  default List<Value> lhsJoinKeyOf(int nodeId) {
    return getJoinKeyOf(nodeId).getLeft();
  }

  default List<Value> rhsJoinKeyOf(int nodeId) {
    return getJoinKeyOf(nodeId).getRight();
  }

  default boolean isEquiJoin(int nodeId) {
    return getJoinKeyOf(nodeId) != null;
  }
}
