package sjtu.ipads.wtune.superopt.substitution;

import com.google.common.graph.MutableValueGraph;
import com.google.common.graph.ValueGraphBuilder;
import org.apache.commons.lang3.tuple.Pair;
import sjtu.ipads.wtune.sqlparser.plan1.PlanNode;
import sjtu.ipads.wtune.superopt.constraint.Constraints;
import sjtu.ipads.wtune.superopt.fragment1.Complexity;
import sjtu.ipads.wtune.superopt.fragment1.FragmentSupport;
import sjtu.ipads.wtune.superopt.fragment1.Symbol;
import sjtu.ipads.wtune.superopt.fragment1.Symbols;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

import static java.util.Arrays.asList;
import static sjtu.ipads.wtune.common.utils.FuncUtils.*;
import static sjtu.ipads.wtune.sqlparser.plan1.PlanSupport.disambiguate;
import static sjtu.ipads.wtune.sqlparser.plan1.PlanSupport.translateAsAst;
import static sjtu.ipads.wtune.superopt.fragment1.FragmentSupport.translateAsPlan;

public class SubstitutionSupport {
  public static Substitution flip(Substitution sub) {
    return Substitution.mk(sub._1(), sub._0(), sub.constraints());
  }

  public static boolean isEligible(Substitution sub) {
    final Symbols symbols = sub._1().symbols();
    final Constraints constraints = sub.constraints();

    for (Symbol.Kind kind : Symbol.Kind.values())
      for (Symbol symbol : symbols.symbolsOf(kind))
        if (none(constraints.eqClassOf(symbol), it -> it.ctx() != symbol.ctx())) {
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
    removeMeaningless(bank);
    removeDuplicated(bank);
    removeTransitive(bank);

    return bank;
  }

  public static void printReadable(Substitution substitution) {
    System.out.println(substitution);
    final Pair<PlanNode, PlanNode> pair = translateAsPlan(substitution, false);
    System.out.println(" q0: " + translateAsAst(disambiguate(pair.getLeft())));
    System.out.println(" q1: " + translateAsAst(disambiguate(pair.getRight())));
  }

  public static Constraints partialConstraintsOf(Substitution substitution, boolean lhs) {
    final Symbols ctx = (lhs ? substitution._0() : substitution._1()).symbols();
    return Constraints.mk(
        listFilter(
            substitution.constraints(), it -> all(asList(it.symbols()), sym -> sym.ctx() == ctx)));
  }

  private static void removeMeaningless(SubstitutionBank bank) {
    bank.removeIf(MeaninglessChecker::isMeaningless);
  }

  private static void removeDuplicated(SubstitutionBank bank) {
    final DuplicationChecker checker = new DuplicationChecker(bank);
    final Set<Substitution> duplicated = new HashSet<>(bank.size() >> 2);

    for (Substitution substitution : bank)
      if (checker.isDuplicated(substitution)) {
        duplicated.add(substitution);
      }

    bank.removeAll(duplicated);
  }

  private static void removeTransitive(SubstitutionBank bank) {
    MutableValueGraph<FragmentProbe, Substitution> graph =
        ValueGraphBuilder.directed()
            .expectedNodeCount(bank.size() << 2)
            .allowsSelfLoops(false)
            .build();

    for (Substitution sub : bank) {
      final FragmentProbe lhs = sub.probe(true), rhs = sub.probe(false);
      graph.addNode(lhs);
      graph.addNode(rhs);
      graph.putEdgeValue(lhs, rhs, sub);
    }

    final TransitiveGraph<FragmentProbe, Substitution> g = new TransitiveGraph<>(graph);
    g.breakTransitivity(bank::remove);
  }
}
