package sjtu.ipads.wtune.superopt;

import sjtu.ipads.wtune.superopt.core.Graph;
import sjtu.ipads.wtune.superopt.core.Substitution;
import sjtu.ipads.wtune.superopt.internal.Prove;
import sjtu.ipads.wtune.superopt.internal.Runner;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.logging.LogManager;

import static sjtu.ipads.wtune.superopt.core.Graph.wrap;
import static sjtu.ipads.wtune.superopt.operator.Operator.innerJoin;
import static sjtu.ipads.wtune.superopt.operator.Operator.proj;

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

  public static void main(String[] args) {
    if (args.length >= 1 && "run".equals(args[0])) {
      Runner.build(args).run();

    } else {
      test0();
    }
  }

  private static void test0() {
    final Graph q0 = wrap(proj(innerJoin(null, null))).setup();
    final Graph q1 = wrap(proj(null)).setup();

    final Collection<Substitution> constraints = Prove.proveEq(q0, q1, -1);
    constraints.forEach(System.out::println);
  }
}
