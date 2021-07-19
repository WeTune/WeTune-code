package sjtu.ipads.wtune.superopt.optimizer;

import static sjtu.ipads.wtune.common.utils.FuncUtils.listFlatMap;

import java.util.Collection;
import java.util.List;
import sjtu.ipads.wtune.sqlparser.plan.PlanNode;
import sjtu.ipads.wtune.superopt.optimizer.internal.SubstitutionBankImpl;

public interface SubstitutionBank extends Iterable<Substitution> {
  SubstitutionBank importFrom(Iterable<String> lines, boolean withCheck);

  boolean contains(Substitution sub);

  int count();

  Collection<Substitution> findByFingerprint(String fingerprint);

  default SubstitutionBank importFrom(Iterable<String> lines) {
    return importFrom(lines, true);
  }

  default List<Substitution> findByFingerprint(PlanNode plan) {
    return listFlatMap(Fingerprint.make(plan), this::findByFingerprint);
  }

  static SubstitutionBank make() {
    return SubstitutionBankImpl.build();
  }
}
