package sjtu.ipads.wtune.superopt.plan;

import java.util.Collection;

public interface Placeholders {
  Placeholder getPick(PlanNode node, int ordinal);

  Placeholder getPredicate(PlanNode node, int ordinal);

  Placeholder getTable(PlanNode node, int ordinal);

  Collection<Placeholder> picks();

  Collection<Placeholder> predicates();

  Collection<Placeholder> tables();

  boolean contains(Placeholder placeholder);

  int count();
}
