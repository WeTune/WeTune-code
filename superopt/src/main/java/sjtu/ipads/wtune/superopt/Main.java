package sjtu.ipads.wtune.superopt;

import sjtu.ipads.wtune.superopt.core.Graph;
import sjtu.ipads.wtune.superopt.core.Substitution;
import sjtu.ipads.wtune.superopt.internal.Enumerator;
import sjtu.ipads.wtune.symsolver.core.Solver;
import sjtu.ipads.wtune.symsolver.core.Summary;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.logging.LogManager;

import static java.lang.System.Logger.Level.INFO;
import static sjtu.ipads.wtune.common.utils.FuncUtils.listMap;
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
    final Substitution substitution =
        Substitution.rebuild(
            "Proj(InnerJoin(Input,Input))|Proj(Input)|TableEq(t0,t2);PickEq(c0,c2);PickEq(c1,c3);PickFrom(c0,[t1]);"
                + "PickFrom(c1,[t0]);PickFrom(c2,[t1]);PickFrom(c3,[t2]);Reference(t0,c1,t1,c2)");
    System.out.println(substitution.g0().toInformativeString());
    System.out.println(substitution.g1().toInformativeString());
    substitution.constraints().forEach(System.out::println);
    //    test2();
    //    final Graph q0 = proj(join(null, null)).toGraph("x");
    //    final Graph q1 = proj(null).toGraph("y");
    //    prove(q0, q1);
    //    test0();
  }

  private static void test0() {
    final List<Graph> fragments = Enumerator.enumFragments();
    LOG.log(INFO, "#fragments: {0}", fragments.size());

    fragments.sort(Graph::compareTo);

    //    for (int i = 0; i < 50; i++)
    //      for (int j = i + 1, bound = fragments.size(); j < bound; ++j)
    //        prove(fragments.get(i), fragments.get(j));
  }

  private static Collection<Substitution> doTest(Graph g0, Graph g1, long timeout) {
    try (final Solver solver = Solver.make(g0.semantic(), g1.semantic(), timeout)) {
      final Collection<Summary> summary = solver.solve();
      return listMap(it -> Substitution.build(g0, g1, Arrays.asList(it.constraints())), summary);
    }
  }

  private static void test1() {
    final Graph q0 = wrap(proj(plainFilter(innerJoin(null, null)))).setup();
    final Graph q1 = wrap(proj(plainFilter(null))).setup();

    final Collection<Substitution> constraints = doTest(q0, q1, -1);
    constraints.forEach(System.out::println);
  }

  private static void test2() {
    final Graph q0 = wrap(proj(innerJoin(null, null))).setup();
    final Graph q1 = wrap(proj(null)).setup();

    final Collection<Substitution> constraints = doTest(q0, q1, -1);
    constraints.forEach(System.out::println);
  }

  private static void prove(Graph g0, Graph g1) {
    if (g0 == g1 || System.identityHashCode(g0) > System.identityHashCode(g1)) return;

    try {
      final Collection<Substitution> results;
      synchronized (g0) {
        synchronized (g1) {
          results = doTest(g0, g1, 300000);
        }
      }

      if (!results.isEmpty()) {
        synchronized (out) {
          out.println("====");
          out.println(g0);
          out.println(g1);
          results.forEach(out::println);
          out.flush();
        }
      }

    } catch (Throwable ex) {
      synchronized (err) {
        err.println("====");
        err.println(g0);
        err.println(g1);
        ex.printStackTrace(err);
        err.flush();
      }
    }
  }
}
