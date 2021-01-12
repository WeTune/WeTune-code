package sjtu.ipads.wtune.superopt.internal;

import sjtu.ipads.wtune.superopt.core.Graph;
import sjtu.ipads.wtune.superopt.core.Substitution;
import sjtu.ipads.wtune.symsolver.core.Constraint;
import sjtu.ipads.wtune.symsolver.core.Indexed;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static sjtu.ipads.wtune.common.utils.FuncUtils.*;
import static sjtu.ipads.wtune.superopt.internal.Placeholder.numberPlaceholder;

public class SubstitutionImpl implements Substitution {
  private final Graph g0, g1;
  private final List<Constraint> constraints;

  private SubstitutionImpl(Graph g0, Graph g1, List<Constraint> constraints) {
    this.g0 = g0;
    this.g1 = g1;
    this.constraints = constraints;
  }

  public static Substitution build(Graph g0, Graph g1, List<Constraint> constraints) {
    numberPlaceholder(g0, g1);
    return new SubstitutionImpl(g0, g1, listMap(it -> it.unwrap(Placeholder.class), constraints));
  }

  public static Substitution build(String str) {
    final String[] split = str.split("\\|");
    if (split.length != 3)
      throw new IllegalArgumentException("invalid serialized substitution: " + str);

    final Graph g0 = Graph.rebuild(split[0]), g1 = Graph.rebuild(split[1]);
    final Map<String, Placeholder> placeholders = numberPlaceholder(g0, g1);

    final List<Constraint> constraints =
        listMap(it -> rebuildConstraint(it, placeholders), split[2].split(";"));

    return new SubstitutionImpl(g0, g1, constraints);
  }

  @Override
  public Graph g0() {
    return g0;
  }

  @Override
  public Graph g1() {
    return g1;
  }

  @Override
  public List<Constraint> constraints() {
    return constraints;
  }

  @Override
  public String toString() {
    return "%s|%s|%s"
        .formatted(
            g0.toString(),
            g1.toString(),
            constraints.stream().map(Object::toString).collect(Collectors.joining(";")));
  }

  private static Constraint rebuildConstraint(String str, Map<String, Placeholder> lookup) {
    final String[] split = str.split("[(),\\[\\]]+");
    switch (Constraint.Kind.valueOf(split[0])) {
      case TableEq:
        return Constraint.tableEq(lookup.get(split[1]), lookup.get(split[2]));
      case PickEq:
        return Constraint.pickEq(lookup.get(split[1]), lookup.get(split[2]));
      case PredicateEq:
        return Constraint.predicateEq(lookup.get(split[1]), lookup.get(split[2]));
      case PickFrom:
        return Constraint.pickFrom(
            lookup.get(split[1]), arrayMap(lookup::get, Indexed.class, subArray(split, 2)));
      case Reference:
        return Constraint.reference(
            lookup.get(split[1]), lookup.get(split[2]), lookup.get(split[3]), lookup.get(split[4]));
      default:
        throw new IllegalStateException();
    }
  }
}
