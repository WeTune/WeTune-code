package sjtu.ipads.wtune.superopt.optimization.internal;

import sjtu.ipads.wtune.sqlparser.plan.PlanNode;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class Memo<T> {
  private final Map<T, OptGroup<T>> memo;
  private final Function<PlanNode, T> keyExtractor;

  public Memo(Function<PlanNode, T> keyExtractor) {
    this.keyExtractor = keyExtractor;
    this.memo = new HashMap<>();
  }

  public OptGroup<T> makeGroup(PlanNode node) {
    final T key = keyExtractor.apply(node);
    OptGroup<T> group = memo.get(key);
    if (group != null) return group;

    group = new OptGroup<>(this, keyExtractor);
    group.add(node);
    return group;
  }

  public OptGroup<T> get(PlanNode node) {
    return memo.get(keyExtractor.apply(node));
  }

  public OptGroup<T> get(T key) {
    return memo.get(key);
  }

  public void clear() {
    memo.clear();
  }

  public boolean bind(T key, OptGroup<T> group) {
    // returns: true: the `key` is bound to `group`
    //          false: the `key` is not bound, because there are another group already.
    //                 in this case, the two group is merged
    if (group == null) throw new IllegalArgumentException();

    final OptGroup<T> existing = memo.putIfAbsent(key, group);
    if (existing == null) return true;
    if (existing.equals(group)) return false;

    group.merge(existing);
    assert group.equals(existing);
    return false;
  }
}
