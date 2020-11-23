package sjtu.ipads.wtune.superopt.enumeration;

import sjtu.ipads.wtune.superopt.interpret.Abstraction;
import sjtu.ipads.wtune.superopt.interpret.Interpretation;

import java.util.Set;

public interface EnumerationPolicy<T> {
  Set<T> enumerate(Interpretation interpretation, Abstraction<T> target);
}
