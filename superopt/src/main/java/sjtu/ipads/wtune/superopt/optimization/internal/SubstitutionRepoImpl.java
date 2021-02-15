package sjtu.ipads.wtune.superopt.optimization.internal;

import sjtu.ipads.wtune.superopt.internal.Generalize;
import sjtu.ipads.wtune.superopt.optimization.Substitution;
import sjtu.ipads.wtune.superopt.optimization.SubstitutionRepo;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

import static java.lang.System.Logger.Level.WARNING;
import static sjtu.ipads.wtune.superopt.internal.Runner.LOG;

public class SubstitutionRepoImpl implements SubstitutionRepo {
  private final Set<Substitution> substitutions;

  private SubstitutionRepoImpl() {
    substitutions = new LinkedHashSet<>(256);
  }

  public static SubstitutionRepo build() {
    return new SubstitutionRepoImpl();
  }

  @Override
  public SubstitutionRepo readLines(Iterable<String> lines) {
    for (String line : lines) {
      if (line.charAt(0) == '=') continue;
      try {
        substitutions.add(Substitution.rebuild(line));
      } catch (Exception ex) {
        LOG.log(WARNING, "Malformed serialized substitution: {0}", line);
        LOG.log(WARNING, "Stacktrace: {0}", ex);
      }
    }
    Generalize.generalize(this);
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
    substitutions.add(sub);
    return this;
  }

  @Override
  public void remove(Substitution sub) {
    substitutions.remove(sub);
  }

  @Override
  public Iterator<Substitution> iterator() {
    return substitutions.iterator();
  }
}
