package sjtu.ipads.wtune.prover.utils;

import static java.util.Objects.requireNonNull;

import java.util.AbstractSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

final class CongruentClass<T> extends AbstractSet<T> {
  private final CongruenceImpl<T> congruence;
  private Set<T> elements;

  CongruentClass(CongruenceImpl<T> congruence) {
    this.congruence = congruence;
    this.elements = new HashSet<>();
  }

  @Override
  public boolean add(T t) {
    requireNonNull(t);

    if (congruence.bind(t, this)) return elements.add(t);
    return false;
  }

  void merge(CongruentClass<T> other) {
    assert other.elements != this.elements;
    assert other.congruence == this.congruence;
    // add all plan from `group`
    this.elements.addAll(other);
    // share same collection to sync automatically
    other.elements = this.elements;
  }

  @Override
  public Iterator<T> iterator() {
    return elements.iterator();
  }

  @Override
  public int size() {
    return elements.size();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof CongruentClass)) return false;
    final CongruentClass<?> that = (CongruentClass<?>) o;
    return elements == that.elements;
  }

  @Override
  public int hashCode() {
    return System.identityHashCode(elements);
  }
}
