package sjtu.ipads.wtune.superopt.runner;

import sjtu.ipads.wtune.superopt.substitution.Substitution;

import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;

public class UpgradeSubstitutionBank implements Runner {
  private Path inputFile;
  private Path outputFile;
  private boolean echo;

  @Override
  public void prepare(String[] argStrings) {
    final Args args = Args.parse(argStrings, 1);
    inputFile = Path.of(args.getOptional("-i", String.class, "wtune_data/substitutions.raw"));
    outputFile = Path.of(args.getOptional("-o", String.class, "wtune_data/substitutions.filtered"));
    echo = args.getOptional("-echo", boolean.class, true);
  }

  @Override
  public void run() throws Exception {
    try (final PrintWriter out = new PrintWriter(Files.newOutputStream(outputFile))) {
      for (String line : Files.readAllLines(inputFile)) {
        final Substitution substitution = Substitution.parse(line, true);
        substitution.resetNaming();
        if (echo) System.out.println(substitution);
        out.println(substitution);
      }
    }
  }
}
