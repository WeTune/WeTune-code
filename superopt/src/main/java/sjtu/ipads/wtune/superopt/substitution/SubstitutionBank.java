package sjtu.ipads.wtune.superopt.substitution;

import sjtu.ipads.wtune.sqlparser.plan.PlanNode;
import sjtu.ipads.wtune.superopt.util.Fingerprint;

import java.util.List;
import java.util.Set;

public interface SubstitutionBank extends Set<Substitution> {
  boolean contains(String substitution);

  Iterable<Substitution> ruleOfFingerprint(Fingerprint fingerprint);

  Iterable<Substitution> matchByFingerprint(PlanNode plan);

  static SubstitutionBank parse(List<String> lines) {
    return parse(lines, false);
  }

  static SubstitutionBank parse(List<String> lines, boolean skipCheck) {
    return SubstitutionBankImpl.parse(lines, skipCheck);
  }
}
