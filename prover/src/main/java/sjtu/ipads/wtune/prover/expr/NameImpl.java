package sjtu.ipads.wtune.prover.expr;

import java.util.Objects;

final class NameImpl implements Name {
  private final String name;

  NameImpl(String name) {
    this.name = name;
  }

  @Override
  public String toString() {
    return name;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    NameImpl name1 = (NameImpl) o;
    return Objects.equals(name, name1.name);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name);
  }
}
