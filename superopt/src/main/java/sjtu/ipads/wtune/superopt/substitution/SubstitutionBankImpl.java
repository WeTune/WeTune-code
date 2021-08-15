package sjtu.ipads.wtune.superopt.substitution;

import java.util.*;

import static sjtu.ipads.wtune.superopt.substitution.SubstitutionSupport.flip;
import static sjtu.ipads.wtune.superopt.substitution.SubstitutionSupport.isEligible;

class SubstitutionBankImpl extends AbstractSet<Substitution> implements SubstitutionBank {
  private final List<Substitution> substitutions;
  private final Set<String> known;

  SubstitutionBankImpl() {
    this.substitutions = new ArrayList<>(2048);
    this.known = new HashSet<>(2048);
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
    if (!known.add(substitution.toString())) return false;
    return substitutions.add(substitution);
  }

  @Override
  public boolean contains(Object o) {
    if (!(o instanceof Substitution)) return false;
    return known.contains(o.toString());
  }

  @Override
  public boolean contains(String substitution) {
    return known.contains(substitution);
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
