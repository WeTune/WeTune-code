package sjtu.ipads.wtune.superopt.substitution;

import sjtu.ipads.wtune.sqlparser.plan1.PlanNode;

import java.util.List;
import java.util.Set;

public interface SubstitutionBank extends Set<Substitution> {
  boolean contains(String substitution);

  Iterable<Substitution> matchByFingerprint(PlanNode plan);

  static SubstitutionBank parse(List<String> lines) {
    return SubstitutionBankImpl.parse(lines);
  }
}
