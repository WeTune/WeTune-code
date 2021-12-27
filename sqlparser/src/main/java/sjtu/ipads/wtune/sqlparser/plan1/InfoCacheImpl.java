package sjtu.ipads.wtune.sqlparser.plan1;

import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import org.apache.commons.lang3.tuple.Pair;
import sjtu.ipads.wtune.common.utils.COW;

import java.util.List;

public class InfoCacheImpl implements InfoCache {
  private final COW<TIntObjectMap<Pair<List<Value>, List<Value>>>> joinKeys;

  InfoCacheImpl() {
    this.joinKeys = new COW<>(new TIntObjectHashMap<>(), null);
  }

  InfoCacheImpl(InfoCacheImpl toCopy) {
    this.joinKeys = new COW<>(toCopy.joinKeys.forRead(), TIntObjectHashMap::new);
  }

  @Override
  public void setJoinKeyOf(int nodeId, List<Value> lhsKeys, List<Value> rhsKeys) {
    joinKeys.forWrite().put(nodeId, Pair.of(lhsKeys, rhsKeys));
  }

  @Override
  public Pair<List<Value>, List<Value>> joinKeyOf(int nodeId) {
    return joinKeys.forRead().get(nodeId);
  }

  void deleteNode(int nodeId) {
    if (joinKeys.forRead().containsKey(nodeId)) joinKeys.forWrite().remove(nodeId);
  }

  void renumberNode(int from, int to) {
    final var keys = joinKeys.forRead().get(from);
    if (keys != null) joinKeys.forWrite().put(to, keys);
  }
}
