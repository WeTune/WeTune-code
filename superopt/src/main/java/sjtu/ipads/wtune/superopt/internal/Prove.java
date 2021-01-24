package sjtu.ipads.wtune.superopt.internal;

import sjtu.ipads.wtune.superopt.core.Graph;
import sjtu.ipads.wtune.superopt.core.Substitution;
import sjtu.ipads.wtune.symsolver.core.Solver;
import sjtu.ipads.wtune.symsolver.core.Summary;

import java.util.Collection;

import static java.util.Arrays.asList;
import static sjtu.ipads.wtune.common.utils.FuncUtils.listMap;

public class Prove {
  public static Collection<Substitution> proveEq(Graph g0, Graph g1, int timeout) {
    try (final Solver solver = Solver.make(g0.semantic(), g1.semantic(), timeout)) {
      final Collection<Summary> summary = solver.solve();
      return listMap(it -> Substitution.build(g0, g1, asList(it.constraints())), summary);

    } catch (Throwable ex) {
      throw new RuntimeException(ex);
    }
  }
}
