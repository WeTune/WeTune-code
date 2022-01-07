package sjtu.ipads.wtune.superopt.substitution;

import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import sjtu.ipads.wtune.superopt.util.Fingerprint;

import java.util.*;

import static sjtu.ipads.wtune.superopt.substitution.SubstitutionSupport.isEligible;

class SubstitutionBankImpl extends AbstractSet<Substitution> implements SubstitutionBank {
  private final Set<Substitution> substitutions;
  private final Set<String> known;
  private final Multimap<String, Substitution> fingerprintIndex;

  SubstitutionBankImpl() {
    this.substitutions = new LinkedHashSet<>(2048);
    this.known = new HashSet<>(2048);
    this.fingerprintIndex = MultimapBuilder.hashKeys(2048).arrayListValues(32).build();
  }

  static SubstitutionBank parse(List<String> lines, boolean skipCheck) {
    final SubstitutionBank bank = new SubstitutionBankImpl();

    for (String line : lines) {
      if (line.isEmpty() || !Character.isLetter(line.charAt(0))) continue;

      final Substitution substitution = Substitution.parse(line);
      if (skipCheck || isEligible(substitution)) bank.add(substitution);
    }

    return bank;
  }

  @Override
  public Iterator<Substitution> iterator() {
    return substitutions.iterator();
  }

  @Override
  public int size() {
    return substitutions.size();
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
  public boolean contains(Object o) {
    if (!(o instanceof Substitution)) return false;
    return known.contains(((Substitution) o).canonicalStringify());
  }

  @Override
  public boolean contains(String substitution) {
    return known.contains(substitution);
  }

  @Override
  public Iterable<Substitution> ruleOfFingerprint(Fingerprint fingerprint) {
    return fingerprintIndex.get(fingerprint.fingerprint());
  }

  @Override
  public boolean remove(Object o) {
    if (substitutions.remove(o)) {
      final Substitution s = (Substitution) o;
      known.remove(s.canonicalStringify());
      fingerprintIndex.remove(Fingerprint.mk(s._0()), s);
      return true;
    }
    return false;
  }

  @Override
  public boolean removeAll(Collection<?> c) {
    return substitutions.removeAll(c);
  }
}
