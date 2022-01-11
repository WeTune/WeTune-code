package sjtu.ipads.wtune.superopt.substitution;

import sjtu.ipads.wtune.superopt.util.Fingerprint;

import java.util.Collection;
import java.util.function.Predicate;

public interface SubstitutionBank {

  boolean add(Substitution substitution);

  boolean contains(String substitution);

  void remove(Substitution substitution);

  void removeIf(Predicate<Substitution> check);

  Collection<Substitution> rules();

  Iterable<Substitution> ruleOfFingerprint(Fingerprint fingerprint);

  default int size() {
    return rules().size();
  }
}
