package sjtu.ipads.wtune.superopt.substitution;

import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import sjtu.ipads.wtune.superopt.util.Fingerprint;

import java.util.*;
import java.util.function.Predicate;

class SubstitutionBankImpl implements SubstitutionBank {
  private final Set<Substitution> substitutions;
  private final Set<String> known;
  private final Multimap<String, Substitution> fingerprintIndex;

  SubstitutionBankImpl() {
    this.substitutions = new LinkedHashSet<>(2048);
    this.known = new HashSet<>(2048);
    this.fingerprintIndex = MultimapBuilder.hashKeys(2048).arrayListValues(32).build();
  }

  @Override
  public Collection<Substitution> rules() {
    return substitutions;
  }

  @Override
  public boolean add(Substitution substitution) {
    if (!known.add(substitution.canonicalStringify())) return false;
    substitutions.add(substitution);
    substitution.setId(substitutions.size());
    fingerprintIndex.put(Fingerprint.mk(substitution._0()).toString(), substitution);
    return true;
  }

  @Override
  public void remove(Substitution o) {
    if (substitutions.remove(o)) {
      known.remove(o.canonicalStringify());
      fingerprintIndex.remove(Fingerprint.mk(o._0()).toString(), o);
    }
  }

  @Override
  public void removeIf(Predicate<Substitution> check) {
    final Iterator<Substitution> iterator = substitutions.iterator();
    while (iterator.hasNext()) {
      final Substitution rule = iterator.next();
      if (check.test(rule)) {
        iterator.remove();
        known.remove(rule.canonicalStringify());
        fingerprintIndex.remove(Fingerprint.mk(rule._0()).toString(), rule);
      }
    }
  }

  @Override
  public boolean contains(String substitution) {
    return known.contains(substitution);
  }

  @Override
  public Iterable<Substitution> ruleOfFingerprint(Fingerprint fingerprint) {
    return fingerprintIndex.get(fingerprint.fingerprint());
  }
}
