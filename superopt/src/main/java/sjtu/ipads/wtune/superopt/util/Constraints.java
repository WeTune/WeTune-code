package sjtu.ipads.wtune.superopt.util;

import com.google.common.collect.Sets;
import sjtu.ipads.wtune.superopt.fragment.symbolic.Placeholder;
import sjtu.ipads.wtune.symsolver.core.*;

import java.util.*;

public class Constraints implements Iterable<Constraint> {
  private final List<Constraint> constraints;
  private final List<Set<Placeholder>> equivalentClasses;

  private Placeholder[] EMPTY_ARR = new Placeholder[0];

  public Constraints(List<Constraint> constraints) {
    this.constraints = constraints;
    this.equivalentClasses = calcEquivalentClass(constraints);
  }

  @Override
  public Iterator<Constraint> iterator() {
    return constraints.iterator();
  }

  public Placeholder[] sourceOf(Placeholder pick) {
    for (Constraint c : constraints)
      if (c.kind() == Constraint.Kind.PickFrom)
        if (c.involves(pick)) return ((PickFrom<Placeholder, Placeholder>) c).ts();

    return EMPTY_ARR;
  }

  public Collection<Placeholder> equivalenceOf(Placeholder p) {
    for (Set<Placeholder> equivalentClass : equivalentClasses)
      if (equivalentClass.contains(p)) return equivalentClass;
    return Collections.emptyList();
  }

  private static List<Set<Placeholder>> calcEquivalentClass(List<Constraint> constraint) {
    final List<Set<Placeholder>> equivalentClasses = new ArrayList<>();

    for (Constraint c : constraint) {
      Placeholder x, y;
      switch (c.kind()) {
        case TableEq:
          final TableEq<Placeholder> tEq = (TableEq<Placeholder>) c;
          x = tEq.tx();
          y = tEq.ty();
          break;
        case PickEq:
          final PickEq<Placeholder> cEq = (PickEq<Placeholder>) c;
          x = cEq.px();
          y = cEq.py();
          break;
        case PredicateEq:
          final PredicateEq<Placeholder> pEq = (PredicateEq<Placeholder>) c;
          x = pEq.px();
          y = pEq.py();
          break;
        default:
          continue;
      }

      boolean found = false;
      for (Set<Placeholder> eqClass : equivalentClasses)
        if (eqClass.contains(x) || eqClass.contains(y)) {
          found = true;
          eqClass.add(x);
          eqClass.add(y);
          break;
        }

      if (!found) equivalentClasses.add(Sets.newHashSet(x, y));
    }

    return equivalentClasses;
  }
}
