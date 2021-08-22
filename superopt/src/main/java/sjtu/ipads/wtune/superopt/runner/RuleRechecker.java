package sjtu.ipads.wtune.superopt.runner;

import sjtu.ipads.wtune.prover.logic.LogicCtx;
import sjtu.ipads.wtune.superopt.constraint.ConstraintEnumerator;
import sjtu.ipads.wtune.superopt.substitution.Substitution;

import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;

import static sjtu.ipads.wtune.prover.ProverSupport.mkLogicCtx;
import static sjtu.ipads.wtune.superopt.constraint.ConstraintSupport.mkConstraintEnumerator;

public class RuleRechecker implements Runner {
  private Path inputFile;
  private Path outputFile;
  private boolean echo;

  @Override
  public void prepare(String[] argStrings) {
    final Args args = Args.parse(argStrings, 1);
    inputFile = Path.of(args.getOptional("-i", String.class, "wtune_data/substitutions.filtered"));
    outputFile =
        Path.of(
            args.getOptional("-o", String.class, "wtune_data/substitutions.filtered.rechecked"));
    echo = args.getOptional("-echo", boolean.class, true);
  }

  @Override
  public void run() throws Exception {
    int total = 0, filtered = 0;
    try (final PrintWriter out = new PrintWriter(Files.newOutputStream(outputFile))) {
      for (String line : Files.readAllLines(inputFile)) {
        ++total;

        final Substitution substitution = Substitution.parse(line);
        final LogicCtx ctx = mkLogicCtx();
        final ConstraintEnumerator enumerator =
            mkConstraintEnumerator(substitution._0(), substitution._1(), ctx);

        if (!enumerator.prove(substitution.constraints())) {
          ++filtered;
          if (echo) System.out.println(substitution);
        } else {
          out.println(substitution.canonicalStringify());
        }

        ctx.close();
      }
    }
    if (echo) System.out.printf("%d -> %d\n", total, total - filtered);
  }
}
