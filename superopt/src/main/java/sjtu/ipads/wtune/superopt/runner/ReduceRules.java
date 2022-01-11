package sjtu.ipads.wtune.superopt.runner;

import sjtu.ipads.wtune.common.utils.IOSupport;
import sjtu.ipads.wtune.superopt.substitution.Substitution;
import sjtu.ipads.wtune.superopt.substitution.SubstitutionBank;
import sjtu.ipads.wtune.superopt.substitution.SubstitutionSupport;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;

import static java.nio.file.StandardCopyOption.ATOMIC_MOVE;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

public class ReduceRules implements Runner {
  private Path inFile;
  private Path outFile;
  private Path additionalFile;

  @Override
  public void prepare(String[] argStrings) throws IOException {
    final Args args = Args.parse(argStrings, 1);
    inFile = Path.of(args.getOptional("i", String.class, "wtune_data/rules.txt"));
    outFile = Path.of(args.getOptional("o", String.class, "wtune_data/rules.txt"));
    additionalFile = Path.of(args.getOptional("a", String.class, "wtune_data/rules.test.txt"));

    if (!Files.exists(inFile)) throw new IllegalArgumentException("no such file: " + inFile);
    if (!Files.exists(additionalFile)) additionalFile = null;
    if (Files.exists(outFile)) {
      final Path filename = outFile.getFileName();
      final Path bakFile = outFile.getParent().resolve(filename + ".bak");
      Files.move(outFile, bakFile, REPLACE_EXISTING, ATOMIC_MOVE);
      if (outFile.equals(inFile)) inFile = bakFile;
    }
  }

  @Override
  public void run() throws Exception {
    final SubstitutionBank bank = SubstitutionSupport.loadBank(inFile);
    if (additionalFile != null) {
      final SubstitutionBank rules = SubstitutionSupport.loadBank(additionalFile);
      for (Substitution rule : rules.rules()) bank.add(rule);
    }

    final int oldSize = bank.size();
    final SubstitutionBank reducedBank = SubstitutionSupport.reduceBank(bank);
    final int minSize = reducedBank.size();

    System.out.printf("%d -> %d\n", oldSize, minSize);

    try (final PrintWriter out = IOSupport.newPrintWriter(outFile)) {
      for (Substitution rule : reducedBank.rules()) out.println(rule.canonicalStringify());
    }
  }
}
