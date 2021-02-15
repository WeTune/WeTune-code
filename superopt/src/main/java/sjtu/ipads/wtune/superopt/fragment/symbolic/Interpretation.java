package sjtu.ipads.wtune.superopt.fragment.symbolic;

public interface Interpretation<T> {
  T object();

  boolean isCompatible(Interpretation<T> other);
}
