package sjtu.ipads.wtune.superopt;

import sjtu.ipads.wtune.superopt.fragment.Fragment;
import sjtu.ipads.wtune.superopt.fragment.ToASTTranslator;
import sjtu.ipads.wtune.superopt.internal.Prove;
import sjtu.ipads.wtune.superopt.internal.Runner;
import sjtu.ipads.wtune.superopt.optimization.Substitution;
import sjtu.ipads.wtune.superopt.optimization.SubstitutionRepo;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.logging.LogManager;

import static sjtu.ipads.wtune.superopt.fragment.Fragment.wrap;
import static sjtu.ipads.wtune.superopt.fragment.Operator.innerJoin;
import static sjtu.ipads.wtune.superopt.fragment.Operator.proj;

public class Main {

  private static final String LOGGER_CONFIG =
      ".level = FINER\n"
          + "java.util.logging.ConsoleHandler.level = FINER\n"
          + "handlers=java.util.logging.ConsoleHandler\n"
          + "java.util.logging.ConsoleHandler.formatter=java.util.logging.SimpleFormatter\n"
          + "java.util.logging.SimpleFormatter.format=[%1$tm/%1$td %1$tT][%3$10s][%4$s] %5$s %n\n";

  static {
    try {
      LogManager.getLogManager()
          .readConfiguration(new ByteArrayInputStream(LOGGER_CONFIG.getBytes()));
    } catch (IOException ignored) {
    }
  }

  public static void main(String[] args) throws IOException {
    if (args.length >= 1 && "run".equals(args[0])) {
      Runner.build(args).run();

    } else {

      try (final PrintWriter writer =
          new PrintWriter(
              Files.newOutputStream(Paths.get("wtune_data", "augmented_substitution")))) {

        final SubstitutionRepo repo =
            SubstitutionRepo.make()
                .readLines(Files.readAllLines(Paths.get("wtune_data", "substitutions")));
        System.out.println(repo.count());

        for (Substitution substitution : repo) {
          final ToASTTranslator translator =
              ToASTTranslator.build().setNumbering(substitution.numbering());

          writer.println(translator.translate(substitution.g0()));
          writer.println(translator.translate(substitution.g1()));
          writer.println(substitution);
          writer.println("====");
        }
      }
    }
  }

  private static void test0() {
    final Fragment q0 = wrap(proj(innerJoin(null, null))).setup();
    final Fragment q1 = wrap(proj(null)).setup();

    final Collection<Substitution> constraints = Prove.prove(q0, q1, -1);
    constraints.forEach(System.out::println);
  }
}
