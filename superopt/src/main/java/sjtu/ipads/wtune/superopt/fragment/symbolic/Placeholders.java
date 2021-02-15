package sjtu.ipads.wtune.superopt.fragment.symbolic;

import sjtu.ipads.wtune.superopt.fragment.Operator;

import java.util.Collection;

public interface Placeholders {
  Placeholder getPick(Operator node, int ordinal);

  Placeholder getPredicate(Operator node, int ordinal);

  Placeholder getTable(Operator node, int ordinal);

  Collection<Placeholder> picks();

  Collection<Placeholder> predicates();

  Collection<Placeholder> tables();

  boolean contains(Placeholder placeholder);

  int count();
}
