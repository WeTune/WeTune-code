package sjtu.ipads.wtune.superopt.optimizer;

import sjtu.ipads.wtune.sqlparser.plan.PlanNode;
import sjtu.ipads.wtune.superopt.optimizer.internal.SubstitutionBankImpl;

import java.util.Collection;
import java.util.List;

import static sjtu.ipads.wtune.common.utils.FuncUtils.listFlatMap;

public interface SubstitutionBank extends Iterable<Substitution> {
  SubstitutionBank importFrom(Iterable<String> lines);

  SubstitutionBank add(Substitution sub);

  boolean contains(Substitution sub);

  void remove(Substitution sub);

  int count();

  Collection<Substitution> findByFingerprint(String fingerprint);

  default List<Substitution> findByFingerprint(PlanNode plan) {
    return listFlatMap(this::findByFingerprint, Fingerprint.make(plan));
  }

  static SubstitutionBank make() {
    return SubstitutionBankImpl.build();
  }
}
