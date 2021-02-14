package sjtu.ipads.wtune.superopt.optimization.match;

import sjtu.ipads.wtune.superopt.optimization.Operator;
import sjtu.ipads.wtune.superopt.optimization.Substitution;

public interface Matching {
  Operator root();

  Operator matchingPoint();

  Substitution substitution();

  Interpretations interpretations();
}
