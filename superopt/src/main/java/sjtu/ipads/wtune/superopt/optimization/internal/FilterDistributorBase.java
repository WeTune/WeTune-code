package sjtu.ipads.wtune.superopt.optimization.internal;

import com.google.common.collect.Sets;
import sjtu.ipads.wtune.superopt.fragment.Filter;
import sjtu.ipads.wtune.superopt.fragment.symbolic.Placeholder;
import sjtu.ipads.wtune.superopt.util.Constraints;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import static sjtu.ipads.wtune.common.utils.FuncUtils.listFilter;

public abstract class FilterDistributorBase implements FilterDistributor {
  protected FilterDistributor next;
  protected FilterDistribution dist;
  protected List<Filter> targets;

  @Override
  public void setNext(FilterDistributor next) {
    this.next = next;
  }

  protected List<Filter> targetSlots(FilterDistribution dist) {
    return listFilter(it -> isTargetSlot(it, dist), Sets.difference(dist.slots(), dist.assigned()));
  }

  protected boolean isTargetSlot(Filter op, FilterDistribution context) {
    return true;
  }

  protected static List<Filter> findEqClass(
      Placeholder placeholder, Collection<Filter> ops, Constraints constraints) {
    return constraints.equivalenceOf(placeholder).stream()
        .map(Placeholder::owner)
        .filter(ops::contains)
        .map(it -> (Filter) it)
        .collect(Collectors.toList());
  }
}
