package sjtu.ipads.wtune.superopt.optimizer.internal;

import static java.lang.System.Logger.Level.WARNING;
import static sjtu.ipads.wtune.superopt.internal.ProofRunner.LOG;
import static sjtu.ipads.wtune.superopt.util.CostEstimator.compareCost;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import sjtu.ipads.wtune.superopt.internal.Generalization;
import sjtu.ipads.wtune.superopt.optimizer.Substitution;
import sjtu.ipads.wtune.superopt.optimizer.SubstitutionBank;

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
  public SubstitutionBank importFrom(Iterable<String> lines, boolean withCheck) {
    for (String line : lines) {
      if (line.charAt(0) == '=') continue;
      try {
        final Substitution sub = Substitution.rebuild(line);
        substitutions.add(sub);
        if (withCheck) substitutions.add(sub.flip());

      } catch (Exception ex) {
        LOG.log(WARNING, "Malformed serialized substitution: {0}", line);
        LOG.log(WARNING, "Stacktrace: {0}", ex.toString());
      }
    }

    if (withCheck) {
      final List<Substitution> toRemove = new ArrayList<>(substitutions.size() >> 1);
      final Generalization generalization = new Generalization(this);
      for (Substitution substitution : substitutions)
        if (!isEligible(substitution) || generalization.canGeneralize(substitution))
          toRemove.add(substitution);
      substitutions.removeAll(toRemove);
    }

    for (Substitution substitution : substitutions)
      index.put(FragmentFingerprint.make(substitution.g0()), substitution);

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
  public Collection<Substitution> findByFingerprint(String fingerprint) {
    return index.get(fingerprint);
  }

  @Override
  public Iterator<Substitution> iterator() {
    return substitutions.iterator();
  }

  private static boolean isEligible(Substitution sub) {
    return Substitution.isValid(sub) && compareCost(sub.g1(), sub.g0()) <= 0;
  }
}
