package sjtu.ipads.wtune.superopt.substitution;

import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import sjtu.ipads.wtune.sqlparser.plan1.PlanNode;

import java.util.*;

import static sjtu.ipads.wtune.common.utils.FuncUtils.listFlatMap;
import static sjtu.ipads.wtune.superopt.substitution.SubstitutionSupport.flip;
import static sjtu.ipads.wtune.superopt.substitution.SubstitutionSupport.isEligible;

class SubstitutionBankImpl extends AbstractSet<Substitution> implements SubstitutionBank {
  private final List<Substitution> substitutions;
  private final Set<String> known;
  private final Multimap<Fingerprint, Substitution> fingerprintIndex;

  SubstitutionBankImpl() {
    this.substitutions = new ArrayList<>(2048);
    this.known = new HashSet<>(2048);
    this.fingerprintIndex = MultimapBuilder.hashKeys(2048).arrayListValues(32).build();
  }

  static SubstitutionBank parse(List<String> lines) {
    final SubstitutionBank bank = new SubstitutionBankImpl();

    for (String line : lines) {
      if (!Character.isLetter(line.charAt(0))) continue;

      final Substitution substitution = Substitution.parse(line);
      if (isEligible(substitution)) bank.add(substitution);

      final Substitution flipped = flip(substitution);
      if (isEligible(flipped)) bank.add(flipped);
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
    fingerprintIndex.put(Fingerprint.mk(substitution._0()), substitution);
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
  public Iterable<Substitution> matchByFingerprint(PlanNode plan) {
    return listFlatMap(Fingerprint.mk(plan), fingerprintIndex::get);
  }

  @Override
  public boolean remove(Object o) {
    return substitutions.remove(o);
  }

  @Override
  public boolean removeAll(Collection<?> c) {
    return substitutions.removeAll(c);
  }
}
