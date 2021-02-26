package sjtu.ipads.wtune.superopt.optimization.internal;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import sjtu.ipads.wtune.sqlparser.plan.OperatorType;
import sjtu.ipads.wtune.superopt.fragment.Fragment;
import sjtu.ipads.wtune.superopt.fragment.Operator;
import sjtu.ipads.wtune.superopt.fragment.OperatorVisitor;
import sjtu.ipads.wtune.superopt.fragment.symbolic.Placeholder;
import sjtu.ipads.wtune.superopt.fragment.symbolic.Placeholders;
import sjtu.ipads.wtune.superopt.internal.Generalization;
import sjtu.ipads.wtune.superopt.optimization.Substitution;
import sjtu.ipads.wtune.superopt.optimization.SubstitutionBank;
import sjtu.ipads.wtune.superopt.util.Constraints;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

import static java.lang.System.Logger.Level.WARNING;
import static sjtu.ipads.wtune.common.utils.FuncUtils.stream;
import static sjtu.ipads.wtune.sqlparser.plan.OperatorType.InnerJoin;
import static sjtu.ipads.wtune.sqlparser.plan.OperatorType.LeftJoin;
import static sjtu.ipads.wtune.superopt.internal.ProofRunner.LOG;
import static sjtu.ipads.wtune.symsolver.core.Constraint.Kind.*;

public class SubstitutionBankImpl implements SubstitutionBank {
  private final Set<Substitution> substitutions;
  private final Multimap<String, Substitution> index;

  private SubstitutionBankImpl() {
    substitutions = new LinkedHashSet<>(256);
    index = HashMultimap.create(256, 2);
  }

  public static SubstitutionBank build() {
    return new SubstitutionBankImpl();
  }

  @Override
  public SubstitutionBank importFrom(Iterable<String> lines) {
    for (String line : lines) {
      if (line.charAt(0) == '=') continue;
      try {
        add(Substitution.rebuild(line));
      } catch (Exception ex) {
        LOG.log(WARNING, "Malformed serialized substitution: {0}", line);
        LOG.log(WARNING, "Stacktrace: {0}", ex);
      }
    }
    return this;
  }

  @Override
  public boolean contains(Substitution sub) {
    return substitutions.contains(sub);
  }

  @Override
  public int count() {
    return substitutions.size();
  }

  @Override
  public SubstitutionBank add(Substitution sub) {
    final Substitution flipped = sub.flip();
    if (flipped.toString().equals(sub.toString())) return this;
    if (Generalization.canGeneralize(sub, this)) return this;

    if (isEligible(sub)) {
      substitutions.add(sub);
      index.put(FragmentFingerprint.make(sub.g0()), sub);
    }

    if (isEligible(flipped)) {
      substitutions.add(flipped);
      index.put(FragmentFingerprint.make(flipped.g0()), flipped);
    }
    return this;
  }

  @Override
  public void remove(Substitution sub) {
    substitutions.remove(sub);
  }

  @Override
  public Collection<Substitution> findByFingerprint(String fingerprint) {
    return index.get(fingerprint);
  }

  @Override
  public Iterator<Substitution> iterator() {
    return substitutions.iterator();
  }

  private static boolean isEligible(Substitution sub) {

    final Placeholders placeholders = sub.g1().placeholders();
    final Constraints constraints = sub.constraints();

    for (Placeholder table : placeholders.tables())
      if (stream(constraints).noneMatch(it -> it.kind() == TableEq && it.involves(table)))
        return false;

    for (Placeholder pick : placeholders.picks())
      if (stream(constraints).noneMatch(it -> it.kind() == PickEq && it.involves(pick)))
        return false;

    for (Placeholder pred : placeholders.predicates())
      if (stream(constraints).noneMatch(it -> it.kind() == PredicateEq && it.involves(pred)))
        return false;

    // complexity of target shouldn't be greater that source
    return compareComplexity(sub.g1(), sub.g0()) <= 0;
  }

  public static int compareComplexity(Fragment g0, Fragment g1) {
    final int[] count0 = OperatorCounter.count(g0), count1 = OperatorCounter.count(g1);
    int result = 0;

    for (int i = 0, bound = count0.length; i < bound; i++) {
      if (i == LeftJoin.ordinal() || i == InnerJoin.ordinal()) continue;

      if (result < 0 && count0[i] > count1[i]) return 0;
      if (result > 0 && count0[i] < count1[i]) return 0;
      if (count0[i] > count1[i]) result = 1;
      else if (count0[i] < count1[i]) result = -1;
    }

    if (result != 0) return result;

    final int numInnerJoin0 = count0[InnerJoin.ordinal()];
    final int numLeftJoin0 = count0[LeftJoin.ordinal()];
    final int numInnerJoin1 = count1[InnerJoin.ordinal()];
    final int numLeftJoin1 = count1[LeftJoin.ordinal()];
    final int numJoin0 = numInnerJoin0 + numLeftJoin0, numJoin1 = numInnerJoin1 + numLeftJoin1;

    if (numJoin0 < numJoin1) return -1;
    if (numJoin0 > numJoin1) return 1;
    return Integer.signum(numLeftJoin0 - numLeftJoin1);
  }

  private static class OperatorCounter implements OperatorVisitor {
    private final int[] counters = new int[OperatorType.values().length];

    static int[] count(Fragment g0) {
      final OperatorCounter counter = new OperatorCounter();
      g0.acceptVisitor(counter);
      return counter.counters;
    }

    @Override
    public boolean enter(Operator op) {
      ++counters[op.type().ordinal()];
      return true;
    }
  }
}
