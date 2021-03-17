package sjtu.ipads.wtune.superopt.fragment.symbolic;

import java.util.Collection;
import sjtu.ipads.wtune.superopt.fragment.Operator;

public interface Placeholders {
  Placeholder getPick(Operator node, int ordinal);

  Placeholder getPredicate(Operator node, int ordinal);

  Placeholder getTable(Operator node, int ordinal);

  Collection<Placeholder> picks();

  Collection<Placeholder> predicates();

  Collection<Placeholder> tables();

  void remove(Operator op);

  boolean contains(Placeholder placeholder);

  int count();
}
