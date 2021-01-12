package sjtu.ipads.wtune.superopt.core;

import sjtu.ipads.wtune.superopt.internal.SubstitutionImpl;
import sjtu.ipads.wtune.symsolver.core.Constraint;

import java.util.List;

public interface Substitution {
  Graph g0();

  Graph g1();

  List<Constraint> constraints();

  static Substitution build(Graph g0, Graph g1, List<Constraint> constraints) {
    return SubstitutionImpl.build(g0, g1, constraints);
  }

  static Substitution rebuild(String str) {
    return SubstitutionImpl.build(str);
  }
}
