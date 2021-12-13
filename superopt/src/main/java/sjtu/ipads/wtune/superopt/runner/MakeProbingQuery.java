package sjtu.ipads.wtune.superopt.runner;

import org.apache.commons.lang3.tuple.Pair;
import sjtu.ipads.wtune.sqlparser.plan1.PlanContext;
import sjtu.ipads.wtune.superopt.substitution.Substitution;

import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;

import static sjtu.ipads.wtune.superopt.substitution.SubstitutionSupport.translateAsPlan;

public class MakeProbingQuery implements Runner {
  private Path inputFile;
  private Path outputFile;

  @Override
  public void prepare(String[] argStrings) {
    final Args args = Args.parse(argStrings, 1);
    inputFile = Path.of(args.getOptional("-i", String.class, "wtune_data/substitutions"));
    outputFile = Path.of(args.getOptional("-o", String.class, inputFile + ".probing"));
  }

  @Override
  public void run() throws Exception {
    try (final PrintWriter out = new PrintWriter(Files.newOutputStream(outputFile))) {
      for (String line : Files.readAllLines(inputFile)) {
        final Substitution rule = Substitution.parse(line);
        final Pair<PlanContext, PlanContext> pair = translateAsPlan(rule);
        final PlanContext p0 = pair.getLeft(), p1 = pair.getRight();
        // TODO

        out.println("===");
        //        out.println(ast0);
        //        out.println(ast1);
        //        out.print(schema.toDdl("mysql", new StringBuilder()));
        out.flush();
      }
    }
  }
}
