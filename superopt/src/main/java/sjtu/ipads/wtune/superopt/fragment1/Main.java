package sjtu.ipads.wtune.superopt.fragment1;

import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import sjtu.ipads.wtune.prover.logic.LogicProver;
import sjtu.ipads.wtune.prover.normalform.Disjunction;
import sjtu.ipads.wtune.prover.uexpr.UExpr;
import sjtu.ipads.wtune.sqlparser.plan1.PlanNode;
import sjtu.ipads.wtune.sqlparser.plan1.PlanSupport;
import sjtu.ipads.wtune.sqlparser.schema.Schema;
import sjtu.ipads.wtune.superopt.substitution.Substitution;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.function.BiFunction;

import static sjtu.ipads.wtune.prover.ProverSupport.*;
import static sjtu.ipads.wtune.prover.uexpr.UExpr.squash;
import static sjtu.ipads.wtune.sqlparser.plan1.PlanSupport.disambiguate;
import static sjtu.ipads.wtune.superopt.fragment1.FragmentSupport.translateAsPlan;

public class Main {
  private static final int[] TARGETS = {};

  private static int test0(int i, String line) {
    //    if (!line.contains("Left")) return -1;
    //    if (line.contains("Left")) return -1;
    //    if (Arrays.binarySearch(TARGETS, i) < 0) return -1;

    final Substitution sub = Substitution.parse(line);

    System.out.println(i);
    final var pair = translateAsPlan(sub);
    final PlanNode plan0 = disambiguate(pair.getLeft());
    final PlanNode plan1 = disambiguate(pair.getRight());

    System.out.println(PlanSupport.translateAsAst(plan0));
    System.out.println(PlanSupport.translateAsAst(plan1));

    final Schema schema = plan0.context().schema();

    final UExpr expr0 = squash(translateAsUExpr(plan0));
    final UExpr expr1 = squash(translateAsUExpr(plan1));

    final Disjunction d0 = canonizeExpr(normalizeExpr(expr0), schema);
    final Disjunction d1 = canonizeExpr(normalizeExpr(expr1), schema);

    System.out.println(d0);
    System.out.println(d1);

    final LogicProver prover = mkProver(schema);
    final boolean result = prover.prove(d0, d1);
    System.out.println(result);

    return result ? 1 : 0;
    //    return -2;
  }

  public static void main(String[] args) throws IOException {
    final List<String> lines = Files.readAllLines(Paths.get("wtune_data", "filtered_bank"));
    final BiFunction<Integer, String, Integer> test = Main::test0;
    final TIntList failures = new TIntArrayList(80);

    int numSuccess = 0, numFailure = 0, numSkip = 0;

    for (int i = 0; i < lines.size(); i++) {
      final String line = lines.get(i);
      if (line.charAt(0) == '=') continue;
      final Integer result = test.apply(i, line);
      if (result == 1) numSuccess++;
      else if (result == 0) numFailure++;
      else if (result == -1) numSkip++;
      else break;

      if (result == 0) failures.add(i);
    }

    System.out.println("#success: " + numSuccess);
    System.out.println("#failure: " + numFailure);
    System.out.println("#skip: " + numSkip);
    System.out.println("failures: " + failures);
  }
}
