package sjtu.ipads.wtune.superopt.runner;

import sjtu.ipads.wtune.superopt.substitution.SubstitutionBank;
import sjtu.ipads.wtune.superopt.substitution.SubstitutionSupport;

import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;

public class MinimizeSubstitutionBank implements Runner {
  private Path inputFile;
  private Path outputFile;
  private boolean echo;

  @Override
  public void prepare(String[] argStrings) {
    final Args args = Args.parse(argStrings, 1);
    inputFile = Path.of(args.getOptional("-i", String.class, "wtune_data/substitutions.raw"));
    outputFile = Path.of(args.getOptional("-o", String.class, inputFile + ".min"));
    echo = args.getOptional("-echo", boolean.class, true);
  }

  @Override
  public void run() throws Exception {
    final SubstitutionBank bank = SubstitutionSupport.loadBank(inputFile);
    final int oldSize = bank.size();
    final SubstitutionBank minimized = SubstitutionSupport.minimize(bank);
    final int minSize = minimized.size();

    if (echo) System.out.printf("%d -> %d\n", oldSize, minSize);

    try (final PrintWriter out = new PrintWriter(Files.newOutputStream(outputFile))) {
      minimized.forEach(it -> out.println(it.canonicalStringify()));
    }
  }
}
