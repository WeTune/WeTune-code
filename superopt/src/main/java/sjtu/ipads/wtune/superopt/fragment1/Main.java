package sjtu.ipads.wtune.superopt.fragment1;

import sjtu.ipads.wtune.prover.uexpr.UExpr;
import sjtu.ipads.wtune.sqlparser.plan1.PlanNode;
import sjtu.ipads.wtune.superopt.substitution.Substitution;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import static sjtu.ipads.wtune.prover.ProverSupport.translateAsUExpr;
import static sjtu.ipads.wtune.sqlparser.plan1.PlanSupport.disambiguate;
import static sjtu.ipads.wtune.superopt.fragment1.FragmentSupport.translateAsPlan;

public class Main {
  public static void main(String[] args) throws IOException {
    final List<String> lines = Files.readAllLines(Paths.get("wtune_data", "filtered_bank"));
    for (int i = 0; i < lines.size(); i++) {
      final String line = lines.get(i);
      if (line.charAt(0) == '=') continue;
      final Substitution sub = Substitution.parse(line);

      System.out.println(i);
      final PlanNode plan0 = disambiguate(translateAsPlan(sub._0(), sub.constraints()));
      final PlanNode plan1 = disambiguate(translateAsPlan(sub._1(), sub.constraints()));

      final UExpr expr0 = translateAsUExpr(plan0);
      final UExpr expr1 = translateAsUExpr(plan1);

      System.out.println(expr0);
      System.out.println(expr1);
    }
  }
}
