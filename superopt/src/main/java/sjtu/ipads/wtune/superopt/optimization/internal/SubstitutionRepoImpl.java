package sjtu.ipads.wtune.superopt.optimization.internal;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import sjtu.ipads.wtune.superopt.fragment.Fragment;
import sjtu.ipads.wtune.superopt.fragment.symbolic.Placeholder;
import sjtu.ipads.wtune.superopt.fragment.symbolic.Placeholders;
import sjtu.ipads.wtune.superopt.internal.Generalize;
import sjtu.ipads.wtune.superopt.optimization.Substitution;
import sjtu.ipads.wtune.superopt.optimization.SubstitutionRepo;
import sjtu.ipads.wtune.superopt.util.Constraints;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

import static java.lang.System.Logger.Level.WARNING;
import static sjtu.ipads.wtune.common.utils.FuncUtils.stream;
import static sjtu.ipads.wtune.superopt.internal.Runner.LOG;
import static sjtu.ipads.wtune.symsolver.core.Constraint.Kind.*;

public class SubstitutionRepoImpl implements SubstitutionRepo {
  private final Set<Substitution> substitutions;
  private final Multimap<String, Substitution> index;

  private SubstitutionRepoImpl() {
    substitutions = new LinkedHashSet<>(256);
    index = HashMultimap.create(256, 2);
  }

  public static SubstitutionRepo build() {
    return new SubstitutionRepoImpl();
  }

  @Override
  public SubstitutionRepo readLines(Iterable<String> lines) {
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
  public SubstitutionRepo add(Substitution sub) {
    if (Generalize.canGeneralize(sub, this)) return this;
    if (isEligibleTarget(sub.g1(), sub)) {
      substitutions.add(sub);
      index.put(FragmentFingerprint.make(sub.g0()), sub);
    }
    if (isEligibleTarget(sub.g0(), sub)) {
      final Substitution flipped = sub.flip();
      substitutions.add(flipped);
      index.put(FragmentFingerprint.make(flipped.g0()), sub);
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

  private static boolean isEligibleTarget(Fragment g, Substitution sub) {
    final Placeholders placeholders = g.placeholders();
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

    return true;
  }
}
