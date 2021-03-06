package sjtu.ipads.wtune.superopt.optimization.internal;

import sjtu.ipads.wtune.common.utils.FuncUtils;
import sjtu.ipads.wtune.sqlparser.plan.FilterNode;
import sjtu.ipads.wtune.superopt.fragment.Filter;
import sjtu.ipads.wtune.superopt.fragment.Operator;
import sjtu.ipads.wtune.superopt.fragment.symbolic.Interpretations;

import java.util.*;

import static com.google.common.collect.Sets.difference;
import static sjtu.ipads.wtune.common.utils.Commons.newIdentitySet;
import static sjtu.ipads.wtune.sqlparser.plan.OperatorType.SubqueryFilter;

public class FilterDistribution {
  private final Set<FilterNode> pool;
  private final Set<Filter> slots;
  private final Interpretations interpretations;

  private final Set<FilterNode> used;
  private final Set<Filter> assigned;
  private final LinkedList<FilterAssignment> assignments;
  private final List<List<FilterAssignment>> results;

  private final boolean forceFullMatch;

  public FilterDistribution(
      Collection<FilterNode> pool,
      Collection<Filter> slots,
      boolean forceFullMatch,
      Interpretations interpretations) {
    this.pool = newIdentitySet(pool);
    this.slots = newIdentitySet(slots);
    this.interpretations = interpretations;
    this.forceFullMatch = forceFullMatch;
    this.used = Collections.newSetFromMap(new IdentityHashMap<>());
    this.assigned = Collections.newSetFromMap(new IdentityHashMap<>());
    this.assignments = new LinkedList<>();
    this.results = new LinkedList<>();
  }

  public boolean isSatisfiable() {
    final long subNodeCount = pool.stream().filter(it -> it.type() == SubqueryFilter).count();
    final long subOpCount = slots.stream().filter(it -> it.type() == SubqueryFilter).count();

    return subNodeCount >= subOpCount && pool.size() >= slots.size();
  }

  public Set<FilterNode> pool() {
    return pool;
  }

  public Set<Filter> slots() {
    return slots;
  }

  public Interpretations interpretations() {
    return interpretations;
  }

  public Set<FilterNode> used() {
    return used;
  }

  public Set<Filter> assigned() {
    return assigned;
  }

  public void assign(Filter op, List<FilterNode> used) {
    this.assigned.add(op);
    this.used.addAll(used);
    this.assignments.add(new FilterAssignment(op, used));
  }

  public List<FilterAssignment> assignments() {
    return assignments;
  }

  public FilterAssignment assignmentOf(Operator op) {
    return FuncUtils.find(it -> it.op() == op, assignments);
  }

  public void rollback() {
    final FilterAssignment assignment = assignments.pollLast();
    assigned.remove(assignment.op());
    used.removeAll(assignment.used());
  }

  public void saveResult() {
    assert assigned.size() == slots.size();

    if (used.size() < pool.size())
      if (forceFullMatch) return;
      else assignments.add(new FilterAssignment(null, new ArrayList<>(difference(pool, used))));

    results.add(new ArrayList<>(assignments));

    if (used.size() < pool.size()) assignments.pollLast();
  }

  public List<List<FilterAssignment>> results() {
    return results;
  }
}
