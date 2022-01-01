package sjtu.ipads.wtune.superopt.optimizer;

import sjtu.ipads.wtune.superopt.util.Complexity;

import java.util.*;

class MinCostSet implements Set<SubPlan> {
  private final Map<String, SubPlan> subPlans;
  private Complexity minCost;

  MinCostSet() {
    this.subPlans = new HashMap<>();
  }

  @Override
  public boolean add(SubPlan subPlan) {
    final Complexity cost = new PlanComplexity(subPlan.plan(), subPlan.nodeId());
    final int cmp = minCost == null ? -1 : cost.compareTo(minCost);
    // the new plan is more costly, abandon it
    if (cmp > 0) return false;
    // the new plan is cheaper, abandon existing ones
    if (cmp < 0) {
      subPlans.clear();
      minCost = cost;
    }

    return subPlans.putIfAbsent(subPlan.toString(), subPlan) == null;
  }

  @Override
  public boolean remove(Object o) {
    if (!(o instanceof SubPlan)) return false;
    return subPlans.remove(o.toString()) != null;
  }

  @Override
  public boolean containsAll(Collection<?> c) {
    for (Object o : c) if (!contains(o)) return false;
    return true;
  }

  @Override
  public boolean addAll(Collection<? extends SubPlan> c) {
    boolean mutated = false;
    for (SubPlan subPlan : c) mutated |= add(subPlan);
    return mutated;
  }

  @Override
  public boolean retainAll(Collection<?> c) {
    return subPlans.entrySet().removeIf(entry -> c.contains(entry.getValue()));
  }

  @Override
  public boolean removeAll(Collection<?> c) {
    boolean mutated = false;
    for (Object o : c) mutated |= remove(o);
    return mutated;
  }

  @Override
  public void clear() {
    subPlans.clear();
  }

  @Override
  public boolean contains(Object o) {
    if (!(o instanceof SubPlan)) return false;
    return subPlans.containsKey(o.toString());
  }

  @Override
  public Iterator<SubPlan> iterator() {
    return subPlans.values().iterator();
  }

  @Override
  public Object[] toArray() {
    return subPlans.values().toArray();
  }

  @Override
  public <T> T[] toArray(T[] a) {
    return subPlans.values().toArray(a);
  }

  @Override
  public boolean isEmpty() {
    return subPlans.isEmpty();
  }

  @Override
  public int size() {
    return subPlans.size();
  }
}
