package sjtu.ipads.wtune.superopt.substitution;

import java.util.List;
import java.util.Set;

public interface SubstitutionBank extends Set<Substitution> {
  boolean contains(String substitution);

  static SubstitutionBank parse(List<String> lines) {
    return SubstitutionBankImpl.parse(lines);
  }
}
