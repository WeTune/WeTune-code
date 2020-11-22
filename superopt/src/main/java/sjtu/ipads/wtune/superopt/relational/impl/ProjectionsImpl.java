package sjtu.ipads.wtune.superopt.relational.impl;

import sjtu.ipads.wtune.superopt.interpret.Abstraction;
import sjtu.ipads.wtune.superopt.relational.Projections;
import sjtu.ipads.wtune.superopt.relational.Relation;

import java.util.Objects;

public class ProjectionsImpl implements Projections {
  private final Range range;
  private final Abstraction<Relation> from;

  private ProjectionsImpl(Range range, Abstraction<Relation> from) {
    this.range = range;
    this.from = from;
  }

  public static ProjectionsImpl create(Range range, Abstraction<Relation> from) {
    return new ProjectionsImpl(range, from);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    ProjectionsImpl that = (ProjectionsImpl) o;
    return range == that.range && Objects.equals(from, that.from);
  }

  @Override
  public int hashCode() {
    return Objects.hash(range, from);
  }
}
