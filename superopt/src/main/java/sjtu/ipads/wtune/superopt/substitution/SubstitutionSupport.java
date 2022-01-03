package sjtu.ipads.wtune.superopt.substitution;

import org.apache.commons.lang3.tuple.Pair;
import sjtu.ipads.wtune.common.utils.IterableSupport;
import sjtu.ipads.wtune.sql.plan.PlanContext;
import sjtu.ipads.wtune.superopt.constraint.Constraints;
import sjtu.ipads.wtune.superopt.fragment.Symbol;
import sjtu.ipads.wtune.superopt.fragment.Symbols;
import sjtu.ipads.wtune.superopt.util.Complexity;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class SubstitutionSupport {
  public static Substitution flip(Substitution sub) {
    return Substitution.mk(sub._1(), sub._0(), sub.constraints());
  }

  public static boolean isEligible(Substitution sub) {
    final Symbols symbols = sub._1().symbols();
    final Constraints constraints = sub.constraints();

    for (Symbol.Kind kind : Symbol.Kind.values())
      for (Symbol symbol : symbols.symbolsOf(kind))
        if (IterableSupport.none(constraints.eqClassOf(symbol), it -> it.ctx() != symbol.ctx())) {
          return false;
        }

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

  public static void printReadable(Substitution substitution) {
    System.out.println(substitution);
    final Pair<PlanContext, PlanContext> pair = translateAsPlan(substitution);
    System.out.println(" q0: " + pair.getLeft());
    System.out.println(" q1: " + pair.getRight());
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
