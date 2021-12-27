package sjtu.ipads.wtune.sqlparser.plan1;

import org.apache.commons.lang3.tuple.Pair;

import java.util.List;

public interface InfoCache {
  void setJoinKeyOf(int nodeId, List<Value> lhsKeys, List<Value> rhsKeys);

  Pair<List<Value>, List<Value>> joinKeyOf(int nodeId);

  default List<Value> lhsJoinKeyOf(int nodeId) {
    return joinKeyOf(nodeId).getLeft();
  }

  default List<Value> rhsJoinKeyOf(int nodeId) {
    return joinKeyOf(nodeId).getRight();
  }

  default boolean isEquiJoin(int nodeId) {
    return joinKeyOf(nodeId) != null;
  }
}
