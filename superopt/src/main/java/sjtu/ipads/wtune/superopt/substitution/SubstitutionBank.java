package sjtu.ipads.wtune.superopt.substitution;

import sjtu.ipads.wtune.superopt.util.Fingerprint;

import java.util.Collection;
import java.util.function.Predicate;

public interface SubstitutionBank {
  int size();

  boolean add(Substitution substitution);

  boolean contains(Substitution rule);

  void remove(Substitution substitution);

  void removeIf(Predicate<Substitution> check);

  Collection<Substitution> rules();

  Iterable<Substitution> ruleOfFingerprint(Fingerprint fingerprint);
}
