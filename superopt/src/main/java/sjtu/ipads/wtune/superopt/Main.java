package sjtu.ipads.wtune.superopt;

import com.google.common.collect.Lists;
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
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.LogManager;
import java.util.stream.Stream;

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
      out = new PrintWriter(Files.newOutputStream(Paths.get("out")));
      err = new PrintWriter(Files.newOutputStream(Paths.get("err")));
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  static {
    System.setProperty("java.util.concurrent.ForkJoinPool.common.parallelism", "16");
  }

  public static void main(String[] args) {
    System.out.println(Enumerate.enumFragments().size());
//    test0(true);
    //    test2();
    //    test3();
  }

  private static void test0(boolean parallel) {
    final List<Graph> fragments = Enumerate.enumFragments();
    LOG.log(INFO, "#fragments: {0}", fragments.size());

    for (int i = 0, bound = fragments.size(); i < bound; i++) fragments.get(i).setId(i);
    Collections.shuffle(fragments);

    Stream<List<Graph>> stream = Lists.cartesianProduct(fragments, fragments).stream();
    if (parallel) stream = stream.parallel();

    stream.forEach(xs -> doProve(xs.get(0), xs.get(1)));
  }

  private static AtomicInteger i = new AtomicInteger(0);

  private static void doProve(Graph g0, Graph g1) {
    if (g0.id() >= g1.id()) return;

    final int current = i.getAndIncrement();
    if (current % 10 == 0) System.out.println(current);

    try {
      final Collection<Substitution> results = Prove.proveEq(g0, g1, 10000);
      logResult(results);
    } catch (Throwable ex) {
      logError(ex, g0, g1);
    }
  }

  private static void test1() {
    final Graph q0 = wrap(proj(plainFilter(innerJoin(null, null)))).setup();
    final Graph q1 = wrap(proj(plainFilter(null))).setup();

    final Collection<Substitution> constraints = Prove.proveEq(q0, q1, -1);
    constraints.forEach(System.out::println);
  }

  private static void test2() {
    final Graph q0 = wrap(proj(innerJoin(null, null))).setup();
    final Graph q1 = wrap(proj(null)).setup();

    final Collection<Substitution> constraints = Prove.proveEq(q0, q1, -1);
    constraints.forEach(System.out::println);
  }

  private static void test3() {
    final List<Graph> fragments = Enumerate.enumFragments();
    LOG.log(INFO, "#fragments: {0}", fragments.size());
    fragments.forEach(System.out::println);
  }

  private static void logResult(Collection<Substitution> substitutions) {
    if (substitutions == null || substitutions.isEmpty()) return;

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
