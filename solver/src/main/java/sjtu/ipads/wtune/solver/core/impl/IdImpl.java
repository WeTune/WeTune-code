package sjtu.ipads.wtune.solver.core.impl;

import sjtu.ipads.wtune.solver.core.Id;

import java.util.Objects;

public class IdImpl implements Id {
  public static int nextId;

  private int id;

  private IdImpl(int id) {
    this.id = id;
  }

  public static IdImpl create() {
    return new IdImpl(nextId++);
  }

  @Override
  public int number() {
    return id;
  }

  @Override
  public void setNumber(int i) {
    this.id = i;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    IdImpl id1 = (IdImpl) o;
    return id == id1.id;
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }
}
