package sjtu.ipads.wtune.superopt.fragment.symbolic;

public interface Interpretation<T> {
  T object();

  boolean isCompatible(T obj);

  default boolean isCompatible(Interpretation<T> other) {
    return isCompatible(other.object());
  }

  default boolean shouldOverride(T obj) {
    return false;
  }

  default boolean shouldOverride(Interpretation<T> other) {
    return shouldOverride(other.object());
  }
}
