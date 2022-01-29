package sjtu.ipads.wtune.superopt.runner;

import gnu.trove.set.TIntSet;
import sjtu.ipads.wtune.common.utils.Args;
import sjtu.ipads.wtune.sql.plan.PlanContext;
import sjtu.ipads.wtune.superopt.substitution.Substitution;
import sjtu.ipads.wtune.superopt.substitution.SubstitutionSupport;

import java.nio.file.Files;
import java.nio.file.Path;

import static sjtu.ipads.wtune.common.utils.IOSupport.checkFileExists;
import static sjtu.ipads.wtune.sql.plan.PlanSupport.translateAsAst;
import static sjtu.ipads.wtune.superopt.runner.RunnerSupport.parseIndices;

public class InspectRules implements Runner {
  private Path file;
  private TIntSet indices;

  @Override
  public void prepare(String[] argStrings) {
    final Args args = Args.parse(argStrings, 1);
    final String indexRange = args.getOptional("indices", String.class, null);
    final String fileName = args.getOptional("f", "file", String.class, "rules/rules.txt");

    file = RunnerSupport.dataDir().resolve(fileName);
    checkFileExists(file);

    if (indexRange != null) indices = parseIndices(indexRange);
  }

  @Override
  public void run() throws Exception {
    int index = 0;
    for (String line : Files.readAllLines(file)) {
      ++index;
      if (indices == null || indices.contains(index)) {
        final Substitution rule = Substitution.parse(line);
        final var plans = SubstitutionSupport.translateAsPlan(rule);
        final PlanContext source = plans.getLeft(), target = plans.getRight();
        System.out.printf("[%d] %s\n", index, rule);
        System.out.println("q0: " + translateAsAst(source, source.root(), true));
        System.out.println("q1: " + translateAsAst(target, target.root(), true));
      }
    }
  }
}
