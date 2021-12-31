package sjtu.ipads.wtune.sqlparser.plan1;

import org.apache.commons.lang3.tuple.Pair;
import sjtu.ipads.wtune.sqlparser.ast1.constants.JoinKind;

import java.util.List;

public interface InfoCache {
  Pair<List<Value>, List<Value>> getJoinKeyOf(int nodeId);

  Expression getSubqueryExprOf(int nodeId);

  int getSubqueryNodeOf(Expression expr);

  int[] getVirtualExpr(Expression expr);

  JoinKind getJoinKindOf(int nodeId);

  void putJoinKeyOf(int nodeId, List<Value> lhsKeys, List<Value> rhsKeys);

  void putSubqueryExprOf(int nodeId, Expression expr);

  void putVirtualExpr(Expression expr, int... nodes);

  void putJoinKindOf(int nodeId, JoinKind joinKind);

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
