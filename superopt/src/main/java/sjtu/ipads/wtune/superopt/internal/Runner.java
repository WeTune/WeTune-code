package sjtu.ipads.wtune.superopt.internal;

import com.google.common.collect.Lists;
import sjtu.ipads.wtune.superopt.plan.Plan;
import sjtu.ipads.wtune.superopt.substitution.Substitution;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

public class Runner {
  public static final System.Logger LOG = System.getLogger("Enumerator");
  private final Stream<List<Plan>> stream;
  private final int estimatedTotal;
  private final AtomicInteger i;

  private final PrintWriter out;
  private final PrintWriter err;

  public Runner(Stream<List<Plan>> stream, int estimatedTotal) throws IOException {
    this.stream = stream;
    this.estimatedTotal = estimatedTotal;
    this.i = new AtomicInteger(0);

    this.out =
        new PrintWriter(
            Files.newOutputStream(Paths.get(System.getProperty("wetune.superopt.outFile"))));

    this.err =
        new PrintWriter(
            Files.newOutputStream(Paths.get(System.getProperty("wetune.superopt.errFile"))));
  }

  public static Runner build(String[] args) {
    final boolean parallel = args.length >= 2 && Boolean.parseBoolean(args[1]);
    final int partitions = args.length >= 4 ? Integer.parseInt(args[2]) : 1;
    final int partitionKey = args.length >= 4 ? Integer.parseInt(args[3]) : -1;

    final List<Plan> frags = Enumerate.enumFragments();
    for (int i = 0, bound = frags.size(); i < bound; i++) frags.get(i).setId(i);

    final List<List<Plan>> pairs = new ArrayList<>(Lists.cartesianProduct(frags, Lists.reverse(frags)));
    Collections.shuffle(pairs);

    Stream<List<Plan>> stream = pairs.stream();
    if (parallel) stream = stream.parallel();

    if (partitions != 1)
      stream =
          stream.filter(
              xs -> (xs.get(0).id() * frags.size() + xs.get(1).id()) % partitions == partitionKey);

    try {
      final int estimated = ((frags.size() * (frags.size() - 1)) >> 1) / partitions;
      return new Runner(stream, estimated);

    } catch (IOException ioe) {
      throw new RuntimeException(ioe);
    }
  }

  public void run() {
    stream.forEach(xs -> doProve(xs.get(0), xs.get(1)));
  }

  private void doProve(Plan g0, Plan g1) {
    if (g0.id() >= g1.id()) return;

    final int current = i.getAndIncrement();
    if (current % 10 == 0) LOG.log(System.Logger.Level.INFO, "{0} / {1}", current, estimatedTotal);

    try {
      final Collection<Substitution> results = Prove.proveEq(g0, g1, 10000);
      logResult(results);
    } catch (Throwable ex) {
      logError(ex, g0, g1);
    }
  }

  private void logResult(Collection<Substitution> substitutions) {
    if (substitutions == null || substitutions.isEmpty()) return;

    synchronized (out) {
      out.println("====");
      substitutions.forEach(out::println);
      out.flush();
    }
  }

  private void logError(Throwable ex, Plan g0, Plan g1) {
    synchronized (err) {
      err.println("====");
      err.println(g0);
      err.println(g1);
      ex.printStackTrace(err);
      err.flush();
    }
  }

  
  static {
    System.setProperty(
        "java.util.concurrent.ForkJoinPool.common.parallelism",
        String.valueOf(Runtime.getRuntime().availableProcessors() - 4));
  }
}
