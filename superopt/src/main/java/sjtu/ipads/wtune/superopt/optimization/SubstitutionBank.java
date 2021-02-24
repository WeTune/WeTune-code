package sjtu.ipads.wtune.superopt.optimization;

import sjtu.ipads.wtune.superopt.optimization.internal.SubstitutionBankImpl;

import java.util.Collection;

public interface SubstitutionBank extends Iterable<Substitution> {
  SubstitutionBank importFrom(Iterable<String> lines);

  SubstitutionBank add(Substitution sub);

  boolean contains(Substitution sub);

  void remove(Substitution sub);

  int count();

  Collection<Substitution> findByFingerprint(String fingerprint);

  static SubstitutionBank make() {
    return SubstitutionBankImpl.build();
  }
}
