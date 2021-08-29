package sjtu.ipads.wtune.superopt.runner;

import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import sjtu.ipads.wtune.prover.logic.LogicProver;
import sjtu.ipads.wtune.prover.normalform.Disjunction;
import sjtu.ipads.wtune.prover.uexpr.UExpr;
import sjtu.ipads.wtune.sqlparser.plan.PlanNode;
import sjtu.ipads.wtune.sqlparser.plan.PlanSupport;
import sjtu.ipads.wtune.sqlparser.schema.Schema;
import sjtu.ipads.wtune.superopt.substitution.Substitution;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import static sjtu.ipads.wtune.prover.ProverSupport.*;
import static sjtu.ipads.wtune.prover.logic.LogicProver.Result.EQ;
import static sjtu.ipads.wtune.sqlparser.plan.PlanSupport.disambiguate;
import static sjtu.ipads.wtune.superopt.substitution.SubstitutionSupport.translateAsPlan;

public class RuleRegression implements Runner {
  private String file;
  private int begin;
  private int[] targets;

  @Override
  public void prepare(String[] argString) {
    assert RuleRegression.class.getSimpleName().equals(argString[0]);

    final Args args = Args.parse(argString, 1);

    file = args.getOptional("-f", String.class, "wtune_data/filtered_bank");
    begin = args.getOptional("-a", int.class, 0);

    final String targetsStr = args.getOptional("-T", String.class, null);
    if (targetsStr != null) {
      final String[] split = targetsStr.split(",");
      final TIntList targetsList = TIntArrayList.wrap(targets = new int[split.length]);
      for (String s : split) targetsList.add(Integer.parseInt(s));
    } else {
      targets = null;
    }
  }

  @Override
  public void run() throws IOException {
    final List<String> lines = Files.readAllLines(Paths.get(file));
    final TIntList failures = new TIntArrayList(80);

    int numSuccess = 0, numFailure = 0, numSkip = 0;

    for (int i = begin; i < lines.size(); i++) {
      final String line = lines.get(i);
      if (line.charAt(0) == '=') continue;

      final int result = check(i, line);

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

  private int check(int i, String line) {
    //    if (!line.contains("Left")) return -1;
    //    if (line.contains("Left")) return -1;
    if (targets != null && Arrays.binarySearch(targets, i) < 0) return -1;

    final Substitution sub = Substitution.parse(line);

    final var pair = translateAsPlan(sub, true, true);
    final PlanNode plan0 = disambiguate(pair.getLeft());
    final PlanNode plan1 = disambiguate(pair.getRight());

    System.out.println(i);
    System.out.println(" q0: " + PlanSupport.translateAsAst(plan0));
    System.out.println(" q1: " + PlanSupport.translateAsAst(plan1));

    final UExpr expr0 = translateAsUExpr(plan0); // squash(translateAsUExpr(plan0));
    final UExpr expr1 = translateAsUExpr(plan1); // squash(translateAsUExpr(plan1));

    final Schema schema = plan0.context().schema();
    final Disjunction d0 = canonizeExpr(normalizeExpr(expr0), schema);
    final Disjunction d1 = canonizeExpr(normalizeExpr(expr1), schema);

    System.out.println(" expr0: " + d0);
    System.out.println(" expr1: " + d1);

    final LogicProver prover = mkProver(schema);
    final boolean result = prover.prove(d0, d1) == EQ;
    System.out.println("==> " + result);

    return result ? 1 : 0;
  }
}
