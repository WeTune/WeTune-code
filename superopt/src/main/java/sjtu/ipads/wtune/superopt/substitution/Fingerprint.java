package sjtu.ipads.wtune.superopt.substitution;

import sjtu.ipads.wtune.sqlparser.plan.OperatorType;
import sjtu.ipads.wtune.sqlparser.plan.PlanNode;
import sjtu.ipads.wtune.sqlparser.plan.ProjNode;
import sjtu.ipads.wtune.superopt.fragment.Fragment;
import sjtu.ipads.wtune.superopt.fragment.Op;
import sjtu.ipads.wtune.superopt.fragment.Proj;

import java.util.HashSet;
import java.util.Set;
import java.util.function.BiConsumer;

import static java.lang.Integer.min;
import static sjtu.ipads.wtune.sqlparser.plan.OperatorType.*;

public final class Fingerprint {
  private final String fingerprint;

  Fingerprint(String fingerprint) {
    this.fingerprint = fingerprint;
  }

  public static Fingerprint mk(Fragment fragment) {
    final StringBuilder builder = new StringBuilder();
    mkFingerprint0(fragment.root(), builder, 4);
    return new Fingerprint(builder.toString());
  }

  public static Set<Fingerprint> mk(PlanNode node) {
    final Set<Fingerprint> fingerprints = new HashSet<>();
    for (int limit = 1; limit <= 4; ++limit)
      mkFingerprint0(node, new StringBuilder(), limit, fingerprints);
    return fingerprints;
  }

  private static void mkFingerprint0(Op op, StringBuilder builder, int limit) {
    if (limit <= 0 || op.kind() == INPUT) return;
    builder.append(identifierOf(op.kind(), op.kind() == PROJ && ((Proj) op).isDeduplicated()));
    mkFingerprint0(op.predecessors()[0], builder, limit - 1);
  }

  private static void mkFingerprint0(
      PlanNode node, StringBuilder builder, int limit, Set<Fingerprint> fingerprints) {
    final OperatorType nodeType = node.kind();
    if (limit <= 0 || nodeType == INPUT) {
      fingerprints.add(new Fingerprint(builder.toString()));
      return;
    }

    if (nodeType.isFilter()) {
      final PlanNode predecessor = locateFilterChainPredecessor(node);
      final int[] counts = countFilters(node);
      final int tot = counts[0], subTot = counts[1];
      final int budget = min(tot, limit);

      for (int cnt = 1; cnt <= budget; ++cnt) {
        for (int subCnt = 0, subBudget = min(cnt, subTot); subCnt <= subBudget; ++subCnt) {
          repeatChar(builder, identifierOf(SIMPLE_FILTER, false), cnt - subCnt);
          repeatChar(builder, identifierOf(IN_SUB_FILTER, false), subCnt);
          // Suppose we have P,S,S,S,J. We should not make out PSSJ.

          if (cnt == subCnt && subCnt < subTot)
            fingerprints.add(new Fingerprint(builder.toString()));
          else mkFingerprint0(predecessor, builder, limit - cnt, fingerprints);

          popChars(builder, cnt);
        }
      }

    } else if (nodeType.isJoin()) {
      final PlanNode predecessor = locateJoinTreePredecessor(node);
      final int[] counts = countJoins(node);
      final int total = counts[0], leftJoins = counts[1];
      mkFingerprintForJoin(
          builder,
          total,
          leftJoins,
          limit,
          (b, residual) -> mkFingerprint0(predecessor, b, residual, fingerprints));

    } else {
      builder.append(
          identifierOf(nodeType, nodeType == PROJ && ((ProjNode) node).isDeduplicated()));
      mkFingerprint0(node.predecessors()[0], builder, limit - 1, fingerprints);
    }
  }

  private static int[] countFilters(PlanNode node) {
    PlanNode path = node;
    int total = 0, subquery = 0;
    while (path.kind().isFilter()) {
      ++total;
      if (path.kind() == IN_SUB_FILTER) ++subquery;
      path = path.predecessors()[0];
    }

    return new int[] {total, subquery};
  }

  private static int[] countJoins(PlanNode node) {
    PlanNode path = node;
    int total = 0, leftJoin = 0;
    while (path.kind().isJoin()) {
      ++total;
      if (path.kind() == LEFT_JOIN) ++leftJoin;
      path = path.predecessors()[0];
    }

    return new int[] {total, leftJoin};
  }

  private static PlanNode locateFilterChainPredecessor(PlanNode node) {
    PlanNode path = node;
    while (path.kind().isFilter()) path = path.predecessors()[0];
    return path;
  }

  private static PlanNode locateJoinTreePredecessor(PlanNode node) {
    PlanNode path = node;
    while (path.kind().isJoin()) path = path.predecessors()[0];
    return path;
  }

  private static void mkFingerprintForJoin(
      StringBuilder builder,
      int joins,
      int leftJoins,
      int limit,
      BiConsumer<StringBuilder, Integer> completion) {
    if (limit == 0 || joins == 0) {
      completion.accept(builder, limit);
      return;
    }

    if (leftJoins > 0) {
      builder.append(identifierOf(LEFT_JOIN, false));
      mkFingerprintForJoin(builder, joins - 1, leftJoins - 1, limit - 1, completion);
      popChars(builder, 1);
    }

    builder.append(identifierOf(INNER_JOIN, false));
    mkFingerprintForJoin(builder, joins - 1, leftJoins, limit - 1, completion);
    popChars(builder, 1);
  }

  private static char identifierOf(OperatorType kind, boolean dedup) {
    switch (kind) {
      case PROJ:
        return dedup ? 'q' : 'p';
      case SIMPLE_FILTER:
        return 'f';
      case IN_SUB_FILTER:
        return 's';
      case INNER_JOIN:
        return 'j';
      case LEFT_JOIN:
        return 'l';
      default:
        return '?';
    }
  }

  private static void repeatChar(StringBuilder builder, char c, int count) {
    for (int i = 0; i < count; ++i) builder.append(c);
  }

  private static void popChars(StringBuilder buffer, int count) {
    buffer.delete(buffer.length() - count, buffer.length());
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof Fingerprint)) return false;
    final Fingerprint that = (Fingerprint) o;
    return fingerprint.equals(that.fingerprint);
  }

  @Override
  public int hashCode() {
    return fingerprint.hashCode();
  }

  @Override
  public String toString() {
    return fingerprint;
  }
}
