package sjtu.ipads.wtune.superopt.enumeration;

import sjtu.ipads.wtune.superopt.interpret.Abstraction;
import sjtu.ipads.wtune.superopt.interpret.Interpretation;
import sjtu.ipads.wtune.superopt.operators.Proj;
import sjtu.ipads.wtune.superopt.relational.Projections;
import sjtu.ipads.wtune.superopt.relational.SymbolicColumns;

import java.util.Collections;
import java.util.Set;

public class ProjEnumeration implements EnumerationPolicy<Projections> {
  @Override
  public Set<Projections> enumerate(
      Interpretation interpretation, Abstraction<Projections> target) {
    final Proj proj = (Proj) target.context();
    final SymbolicColumns columns = proj.prev()[0].outSchema().columns(interpretation);
    return Collections.emptySet();
  }
}
