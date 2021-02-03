package sjtu.ipads.wtune.superopt.internal;

import sjtu.ipads.wtune.superopt.plan.Placeholder;
import sjtu.ipads.wtune.superopt.plan.Plan;
import sjtu.ipads.wtune.superopt.plan.internal.Semantic;
import sjtu.ipads.wtune.superopt.substitution.Substitution;
import sjtu.ipads.wtune.superopt.util.PlaceholderNumbering;
import sjtu.ipads.wtune.symsolver.core.Constraint;
import sjtu.ipads.wtune.symsolver.core.Solver;
import sjtu.ipads.wtune.symsolver.core.Summary;
import sjtu.ipads.wtune.symsolver.core.Sym;

import java.util.Arrays;
import java.util.Collection;

import static java.util.Comparator.comparingInt;
import static sjtu.ipads.wtune.common.utils.Commons.coalesce;
import static sjtu.ipads.wtune.common.utils.FuncUtils.listMap;

public class Prove {
  private final Plan g0, g1;
  private final Semantic semantic0, semantic1;

  private PlaceholderNumbering numbering;

  public Prove(Plan g0, Plan g1) {
    this.g0 = g0;
    this.g1 = g1;
    this.semantic0 = g0.semantic();
    this.semantic1 = g1.semantic();
  }

  public static Collection<Substitution> prove(Plan g0, Plan g1, int timeout) {
    return new Prove(g0, g1).prove(timeout);
  }

  public Collection<Substitution> prove(int timeout) {
    final Collection<Summary> summaries;
    try (final Solver solver = Solver.make(semantic0, semantic1, timeout)) {
      if ((summaries = solver.solve()) == null) return null;
    }

    numbering = PlaceholderNumbering.build();
    numbering.number(g0, g1);

    return listMap(this::makeSubstitution, summaries);
  }

  private Substitution makeSubstitution(Summary summary) {
    return Substitution.build(
        g0, g1, numbering, listMap(this::makeConstraint, summary.constraints()));
  }

  private Constraint makeConstraint(Constraint constraint) {
    final Constraint.Kind kind = constraint.kind();
    final Object[] targets = constraint.targets();
    final Placeholder[] placeholders = new Placeholder[targets.length];

    for (int j = 0; j < targets.length; j++) {
      final Sym sym = (Sym) targets[j];
      placeholders[j] = coalesce(semantic0.lookup(sym), semantic1.lookup(sym));
      assert placeholders[j] != null;
    }

    if (constraint.kind() == Constraint.Kind.PickFrom)
      Arrays.sort(placeholders, 1, placeholders.length, comparingInt(numbering::numberOf));
    else if (constraint.kind() != Constraint.Kind.Reference)
      Arrays.sort(placeholders, comparingInt(numbering::numberOf));

    return Constraint.make(kind, placeholders);
  }
}
