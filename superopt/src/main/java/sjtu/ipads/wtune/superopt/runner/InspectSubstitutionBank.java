package sjtu.ipads.wtune.superopt.runner;

import sjtu.ipads.wtune.superopt.substitution.Substitution;

import java.nio.file.Files;
import java.nio.file.Path;

public class InspectSubstitutionBank implements Runner {
  private Path inputFile;
  private boolean echo;

  @Override
  public void prepare(String[] argStrings) {
    final Args args = Args.parse(argStrings, 1);
    inputFile = Path.of(args.getOptional("-i", String.class, "wtune_data/substitutions.filtered"));
    echo = args.getOptional("-echo", boolean.class, true);
  }

  @Override
  public void run() throws Exception {
    int count = 0;
    for (String line : Files.readAllLines(inputFile)) {
      final Substitution substitution = Substitution.parse(line);
      if (substitution._0().toString().equals(substitution._1().toString())) {
        System.out.println(substitution);
        ++count;
      }
    }
    System.out.println(count);
  }
}
