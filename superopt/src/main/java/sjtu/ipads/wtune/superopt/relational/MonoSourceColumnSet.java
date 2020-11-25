package sjtu.ipads.wtune.superopt.relational;

import sjtu.ipads.wtune.superopt.interpret.Abstraction;
import sjtu.ipads.wtune.superopt.relational.impl.MultiSourceColumnSet;

import java.util.Collections;
import java.util.Set;

public interface MonoSourceColumnSet extends ColumnSet {

  Abstraction<ConcreteColumns> abstractions();

  @Override
  MonoSourceColumnSet copy();

  void setId(int id);

  int id();

  default Abstraction<InputSource> source() {
    return null;
  }

  @Override
  default ColumnSet union(ColumnSet other) {
    if (other instanceof MultiSourceColumnSet) return other.union(this);
    else if (this.equals(other)) return this;
    else return MultiSourceColumnSet.from(Set.of(this, (MonoSourceColumnSet) other));
  }

  @Override
  default Set<MonoSourceColumnSet> flatten() {
    return Collections.singleton(this);
  }
}
