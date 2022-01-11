package sjtu.ipads.wtune.superopt.substitution;

import org.apache.commons.lang3.tuple.Pair;
import sjtu.ipads.wtune.sql.plan.PlanContext;
import sjtu.ipads.wtune.superopt.util.Complexity;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class SubstitutionSupport {
  public static Substitution flipRule(Substitution sub) {
    return Substitution.mk(sub._1(), sub._0(), sub.constraints());
  }

  public static boolean isEligible(Substitution sub) {
    final Complexity complexity0 = Complexity.mk(sub._0());
    final Complexity complexity1 = Complexity.mk(sub._1());

    return complexity0.compareTo(complexity1, true) >= 0;
  }

  public static SubstitutionBank loadBank(Path path) throws IOException {
    return loadBank(path, false);
  }

  public static SubstitutionBank loadBank(Path path, boolean skipCheck) throws IOException {
    return SubstitutionBank.parse(Files.readAllLines(path), skipCheck);
  }

  public static SubstitutionBank minimize(SubstitutionBank bank) {
    removeMeaningless(bank);
    removeDuplicated(bank);

    return bank;
  }

  private static void removeMeaningless(SubstitutionBank bank) {
    bank.removeIf(MeaninglessChecker::isMeaningless);
  }

  private static void removeDuplicated(SubstitutionBank bank) {
    final List<Substitution> substitutions = new ArrayList<>(bank);
    for (int i = 0; i < substitutions.size(); i++) {
      System.out.print(i + " ");
      if (i % 10 == 0) System.out.println();
      DuplicationChecker.removeIfDuplicated(bank, substitutions.get(i));
    }
  }

  public static Pair<PlanContext, PlanContext> translateAsPlan(Substitution rule) {
    return new PlanTranslator(rule).translate();
  }

  public static Pair<PlanContext, PlanContext> translateAsPlan2(Substitution rule) {
    return new PlanTranslator2(rule).translate();
  }
}
