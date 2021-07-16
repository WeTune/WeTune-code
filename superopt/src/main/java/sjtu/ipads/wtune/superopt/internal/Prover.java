package sjtu.ipads.wtune.superopt.internal;

import static java.util.Comparator.comparingInt;
import static sjtu.ipads.wtune.common.utils.Commons.coalesce;
import static sjtu.ipads.wtune.common.utils.FuncUtils.listMap;

import java.util.Arrays;
import java.util.Collection;
import sjtu.ipads.wtune.superopt.fragment.Fragment;
import sjtu.ipads.wtune.superopt.fragment.Semantic;
import sjtu.ipads.wtune.superopt.fragment.symbolic.Numbering;
import sjtu.ipads.wtune.superopt.fragment.symbolic.Placeholder;
import sjtu.ipads.wtune.superopt.optimizer.Substitution;
import sjtu.ipads.wtune.symsolver.core.Constraint;
import sjtu.ipads.wtune.symsolver.core.Solver;
import sjtu.ipads.wtune.symsolver.core.Summary;
import sjtu.ipads.wtune.symsolver.core.Sym;

public class Prover {
  private final Fragment g0, g1;
  private final Semantic semantic0, semantic1;

  private Numbering numbering;

  public Prover(Fragment g0, Fragment g1) {
    this.g0 = g0;
    this.g1 = g1;
    this.semantic0 = g0.semantic();
    this.semantic1 = g1.semantic();
  }

  public static Collection<Substitution> prove(Fragment g0, Fragment g1, int timeout) {
    return new Prover(g0, g1).prove(timeout);
  }

  public Collection<Substitution> prove(int timeout) {
    final Collection<Summary> summaries;
    try (final Solver solver = Solver.make(semantic0, semantic1, timeout)) {
      if ((summaries = solver.solve()) == null) return null;
    }

    numbering = Numbering.make().number(g0, g1);

    return listMap(summaries, this::makeSubstitution);
  }

  private Substitution makeSubstitution(Summary summary) {
    return Substitution.make(
        g0, g1, numbering, listMap(this::makeConstraint, summary.constraints()));
  }

  private Constraint makeConstraint(Constraint constraint) {
    // We need to reconstruct the constraint by replacing the
    // Sym in the raw constraint by Placeholder
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
