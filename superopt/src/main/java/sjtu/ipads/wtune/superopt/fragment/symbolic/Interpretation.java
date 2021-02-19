package sjtu.ipads.wtune.superopt.fragment.symbolic;

public interface Interpretation<T> {
  T object();

  boolean isCompatible(T obj);

  default boolean isCompatible(Interpretation<T> other) {
    return isCompatible(other.object());
  }
}
