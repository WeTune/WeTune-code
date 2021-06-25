package sjtu.ipads.wtune.prover.utils;

import static java.util.Objects.requireNonNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

class CongruenceImpl<T> implements Congruence<T> {
  private final Map<T, CongruentClass<T>> classes;

  protected CongruenceImpl() {
    classes = new HashMap<>();
  }

  boolean bind(T t, CongruentClass<T> cls) {
    // returns: true: the `key` is bound to `group`
    //          false: the `key` is not bound, because there are another group already.
    //                 in this case, the two group is merged
    if (cls == null) throw new IllegalArgumentException();

    final CongruentClass<T> existing = classes.putIfAbsent(t, cls);
    if (existing == null) return true;
    if (existing.equals(cls)) return false;

    cls.merge(existing);
    assert cls.equals(existing);

    return false;
  }

  @Override
  public Set<T> makeClass(T x) {
    requireNonNull(x);

    CongruentClass<T> congruentClass = classes.get(x);
    if (congruentClass != null) return congruentClass;

    congruentClass = new CongruentClass<>(this);
    congruentClass.add(x);
    return congruentClass;
  }

  @Override
  public Set<T> getClass(T x) {
    return classes.get(x);
  }

  @Override
  public void putCongruent(T x, T y) {
    makeClass(x).add(y);
  }

  @Override
  public boolean isCongruent(T x, T y) {
    if (Objects.equals(x, y)) return true; // reflexivity
    if (x == null || y == null) return false;

    final CongruentClass<T> gx = classes.get(x), gy = classes.get(y);
    return gx != null && gx.equals(gy);
  }
}
