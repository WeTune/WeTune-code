package sjtu.ipads.wtune.superopt.operators;

import sjtu.ipads.wtune.superopt.relational.Projections;
import sjtu.ipads.wtune.superopt.interpret.Abstraction;
import sjtu.ipads.wtune.superopt.operators.impl.ProjImpl;

public interface Proj extends Operator {
  Abstraction<Projections> projs();

  static Proj create() {
    return ProjImpl.create();
  }
}
