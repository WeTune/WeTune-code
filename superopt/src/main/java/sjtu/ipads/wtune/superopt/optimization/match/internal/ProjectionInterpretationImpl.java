package sjtu.ipads.wtune.superopt.optimization.match.internal;

import sjtu.ipads.wtune.sqlparser.relational.Attribute;
import sjtu.ipads.wtune.superopt.optimization.match.ProjectionInterpretation;

import java.util.List;

public class ProjectionInterpretationImpl implements ProjectionInterpretation {
  private final List<Attribute> projs;

  public ProjectionInterpretationImpl(List<Attribute> projs) {
    this.projs = projs;
  }

  @Override
  public List<Attribute> projection() {
    return projs;
  }

  boolean isCompatible(List<Attribute> otherProjs) {
    if (projs.size() != otherProjs.size()) return false;

    for (int i = 0, bound = otherProjs.size(); i < bound; i++)
      if (projs.get(i) != otherProjs.get(i)) return false;

    return true;
  }
}
