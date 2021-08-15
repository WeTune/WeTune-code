package sjtu.ipads.wtune.superopt.substitution;

import sjtu.ipads.wtune.sqlparser.plan.OperatorType;
import sjtu.ipads.wtune.superopt.constraint.Constraints;
import sjtu.ipads.wtune.superopt.fragment1.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

import static sjtu.ipads.wtune.common.utils.FuncUtils.none;

public class SubstitutionSupport {
  public static Substitution flip(Substitution sub) {
    return Substitution.mk(sub._1(), sub._0(), sub.constraints());
  }

  public static boolean isEligible(Substitution sub) {
    final Symbols symbols = sub._1().symbols();
    final Constraints constraints = sub.constraints();

    for (Symbol.Kind kind : Symbol.Kind.values())
      for (Symbol symbol : symbols.symbolsOf(kind))
        if (!isProjOutAttrs(symbol, symbols)
            && none(constraints.eqClassOf(symbol), it -> it.ctx() != symbol.ctx())) {
          return false;
        }

    final Complexity complexity0 = FragmentSupport.calcComplexity(sub._0());
    final Complexity complexity1 = FragmentSupport.calcComplexity(sub._1());

    return complexity0.compareTo(complexity1, true) >= 0;
  }

  public static SubstitutionBank loadBank(Path path) throws IOException {
    return SubstitutionBank.parse(Files.readAllLines(path));
  }

  public static SubstitutionBank minimize(SubstitutionBank bank) {
    final DuplicationChecker checker = new DuplicationChecker(bank);
    final Set<Substitution> duplicated = new HashSet<>(bank.size() >> 2);

    for (Substitution substitution : bank)
      if (checker.isDuplicated(substitution)) {
        duplicated.add(substitution);
      }

    bank.removeAll(duplicated);
    return bank;
  }

  private static boolean isProjOutAttrs(Symbol symbol, Symbols symbols) {
    if (symbol.kind() != Symbol.Kind.ATTRS) return false;

    final Op owner = symbols.ownerOf(symbol);
    return owner.kind() == OperatorType.PROJ && ((Proj) owner).outAttrs() == symbol;
  }
}
