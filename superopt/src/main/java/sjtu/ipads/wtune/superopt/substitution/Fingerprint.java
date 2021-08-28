package sjtu.ipads.wtune.superopt.substitution;

import sjtu.ipads.wtune.sqlparser.plan.OperatorType;
import sjtu.ipads.wtune.sqlparser.plan.PlanNode;
import sjtu.ipads.wtune.superopt.fragment.Fragment;
import sjtu.ipads.wtune.superopt.fragment.Op;
import sjtu.ipads.wtune.superopt.fragment.Proj;

import java.util.List;

import static java.util.Collections.emptyList;
import static sjtu.ipads.wtune.sqlparser.plan.OperatorType.PROJ;

record Fingerprint(String fingerprint) {
  static Fingerprint mk(Fragment fragment) {
    final StringBuilder builder = new StringBuilder();
    mkFingerprint0(fragment.root(), builder);
    return new Fingerprint(builder.toString());
  }

  static List<Fingerprint> mk(PlanNode node) {
    return emptyList(); // TODO
  }

  private static void mkFingerprint0(Op op, StringBuilder builder) {
    if (op.kind() == OperatorType.INPUT) return;
    builder.append(identifierOf(op.kind(), op.kind() == PROJ && ((Proj) op).isDeduplicated()));
  }

  private static char identifierOf(OperatorType kind, boolean dedup) {
    return switch (kind) {
      case PROJ -> dedup ? 'q' : 'p';
      case SIMPLE_FILTER -> 'f';
      case IN_SUB_FILTER -> 's';
      case INNER_JOIN -> 'j';
      case LEFT_JOIN -> 'l';
      default -> '?';
    };
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof Fingerprint that)) return false;
    return fingerprint.equals(that.fingerprint);
  }

  @Override
  public int hashCode() {
    return fingerprint.hashCode();
  }
}
