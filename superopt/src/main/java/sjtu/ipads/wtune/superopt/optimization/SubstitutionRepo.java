package sjtu.ipads.wtune.superopt.optimization;

import sjtu.ipads.wtune.superopt.optimization.internal.SubstitutionRepoImpl;

import java.util.Collection;

public interface SubstitutionRepo extends Iterable<Substitution> {
  SubstitutionRepo readLines(Iterable<String> lines);

  SubstitutionRepo add(Substitution sub);

  boolean contains(Substitution sub);

  void remove(Substitution sub);

  int count();

  Collection<Substitution> findByFingerprint(String fingerprint);

  static SubstitutionRepo make() {
    return SubstitutionRepoImpl.build();
  }
}
