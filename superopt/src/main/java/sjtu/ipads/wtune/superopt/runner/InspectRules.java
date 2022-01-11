package sjtu.ipads.wtune.superopt.runner;

import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import sjtu.ipads.wtune.sql.plan.PlanContext;
import sjtu.ipads.wtune.superopt.substitution.Substitution;
import sjtu.ipads.wtune.superopt.substitution.SubstitutionSupport;

import java.nio.file.Files;
import java.nio.file.Path;

import static sjtu.ipads.wtune.sql.plan.PlanSupport.translateAsAst;

public class InspectRules implements Runner {
  private Path file;
  private TIntSet indices;

  @Override
  public void prepare(String[] argStrings) {
    final Args args = Args.parse(argStrings, 1);
    final String indexRange = args.getOptional("indices", String.class, null);
    file = Path.of(args.getOptional("f", String.class, "wtune_data/rules.txt"));

    if (indexRange != null) {
      final String[] ranges = indexRange.split(",");
      indices = new TIntHashSet();
      for (String range : ranges) {
        if (range.isEmpty()) {
          throw new IllegalArgumentException("invalid index range: " + indexRange);
        }

        final String[] fields = range.split("-");

        try {
          if (fields.length == 1) {
            indices.add(Integer.parseInt(fields[0]));
          } else if (fields.length == 2) {
            final int begin = Integer.parseInt(fields[0]);
            final int end = Integer.parseInt(fields[1]);
            for (int i = begin; i < end; ++i) indices.add(i);
          }
          continue;

        } catch (NumberFormatException ignored) {
        }

        throw new IllegalArgumentException("invalid index range: " + indexRange);
      }
    }
  }

  @Override
  public void run() throws Exception {
    int index = 0;
    for (String line : Files.readAllLines(file)) {
      if (indices == null || indices.contains(index)) {
        final Substitution rule = Substitution.parse(line);
        final var plans = SubstitutionSupport.translateAsPlan(rule);
        final PlanContext source = plans.getLeft(), target = plans.getRight();
        System.out.printf("[%d] %s\n", index, rule);
        System.out.println("q0: " + translateAsAst(source, source.root(), true));
        System.out.println("q1: " + translateAsAst(target, target.root(), true));
      }
      ++index;
    }
  }
}
