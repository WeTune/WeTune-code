package sjtu.ipads.wtune.superopt.enumeration;

import sjtu.ipads.wtune.superopt.interpret.Abstraction;
import sjtu.ipads.wtune.superopt.interpret.Interpretation;
import sjtu.ipads.wtune.superopt.relational.GroupKeys;
import sjtu.ipads.wtune.superopt.relational.Projections;
import sjtu.ipads.wtune.superopt.relational.SortKeys;

import java.util.Set;

public interface EnumerationPolicy<T> {
  Set<T> enumerate(Interpretation interpretation, Abstraction<T> target);

  static EnumerationPolicy<Projections> projectionPolicy() {
    return ProjectionsEnumerationPolicy.create();
  }

  static EnumerationPolicy<SortKeys> sortKeysPolicy() {
    return SortKeysEnumerationPolicy.create();
  }

  static EnumerationPolicy<GroupKeys> groupKeysPolicy() {
    return GroupKeysEnumerationPolicy.create();
  }
}
