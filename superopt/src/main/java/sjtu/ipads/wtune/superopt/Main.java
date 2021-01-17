package sjtu.ipads.wtune.superopt;

import sjtu.ipads.wtune.superopt.core.Graph;
import sjtu.ipads.wtune.superopt.core.Substitution;
import sjtu.ipads.wtune.superopt.internal.Enumerate;
import sjtu.ipads.wtune.superopt.internal.Prove;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;
import java.util.logging.LogManager;

import static java.lang.System.Logger.Level.INFO;
import static sjtu.ipads.wtune.superopt.core.Graph.wrap;
import static sjtu.ipads.wtune.superopt.operator.Operator.*;

public class Main {
  private static final System.Logger LOG = System.getLogger("Enumerator");

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

  private static final PrintWriter out;
  private static final PrintWriter err;

  static {
    try {
      out = new PrintWriter(Files.newOutputStream(Paths.get("/home/cleveland/var/out")));
      err = new PrintWriter(Files.newOutputStream(Paths.get("/home/cleveland/var/err")));
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  static {
    System.setProperty("java.util.concurrent.ForkJoinPool.common.parallelism", "8");
  }

  public static void main(String[] args) {
    test2();
    //    for (int i = 0; i < 10; i++) {
    //      test2();
    //    }
    //    final Graph q0 = proj(join(null, null)).toGraph("x");
    //    final Graph q1 = proj(null).toGraph("y");
    //    prove(q0, q1);
    //    test0();
  }

  private static void test0() {
    final List<Graph> fragments = Enumerate.enumFragments();
    LOG.log(INFO, "#fragments: {0}", fragments.size());

    fragments.sort(Graph::compareTo);

    for (int i = 1; i < fragments.size(); i++) {
      if (i % 10 == 0) System.out.println(i);
      final Graph g0 = fragments.get(0);
      final Graph g1 = fragments.get(i);
      try {
        final Collection<Substitution> results = Prove.prove(g0, g1, 300000);
        logResult(results);
      } catch (Throwable ex) {
        logError(ex, g0, g1);
      }
    }
  }

  private static void test1() {
    final Graph q0 = wrap(proj(plainFilter(innerJoin(null, null)))).setup();
    final Graph q1 = wrap(proj(plainFilter(null))).setup();

    final Collection<Substitution> constraints = Prove.prove(q0, q1, -1);
    constraints.forEach(System.out::println);
  }

  private static void test2() {
    final Graph q0 = wrap(proj(innerJoin(null, null))).setup();
    final Graph q1 = wrap(proj(null)).setup();

    final Collection<Substitution> constraints = Prove.prove(q0, q1, -1);
    constraints.forEach(System.out::println);
  }

  private static void logResult(Collection<Substitution> substitutions) {
    synchronized (out) {
      out.println("====");
      substitutions.forEach(out::println);
      out.flush();
    }
  }

  private static void logError(Throwable ex, Graph g0, Graph g1) {
    synchronized (err) {
      err.println("====");
      err.println(g0);
      err.println(g1);
      ex.printStackTrace(err);
      err.flush();
    }
  }
}
