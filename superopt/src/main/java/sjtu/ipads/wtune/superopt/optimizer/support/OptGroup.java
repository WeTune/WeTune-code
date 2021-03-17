package sjtu.ipads.wtune.superopt.optimizer.support;

import java.util.AbstractList;
import java.util.List;
import java.util.function.Function;
import sjtu.ipads.wtune.sqlparser.plan.PlanNode;

public class OptGroup<T> extends AbstractList<PlanNode> {
  private final Memo<T> memo;
  private final Function<PlanNode, T> keyExtractor;
  private List<PlanNode> opts; // min cost list

  OptGroup(Memo<T> memo, Function<PlanNode, T> keyExtractor) {
    this.memo = memo;
    this.keyExtractor = keyExtractor;
    this.opts = new MinCostList();
  }

  @Override
  public boolean add(PlanNode node) {
    final T key = keyExtractor.apply(node);
    if (memo.bind(key, this)) return opts.add(node);
    // when `bind` returns false, this group must have been merged with another group
    // bound with the `key`, so no need to call `add0`
    return false;
  }

  public void merge(OptGroup<T> group) {
    assert group.opts != this.opts;
    // add all plan from `group`
    group.forEach(opts::add);
    // share same collection to sync automatically
    group.opts = this.opts;
  }

  @Override
  public PlanNode get(int index) {
    return opts.get(index);
  }

  @Override
  public int size() {
    return opts.size();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    return this.opts == ((OptGroup<?>) o).opts;
  }

  @Override
  public int hashCode() {
    return System.identityHashCode(this.opts);
  }
}
