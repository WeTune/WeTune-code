package sjtu.ipads.wtune.superopt.runner;

import me.tongfei.progressbar.ProgressBar;
import org.apache.commons.lang3.tuple.Pair;
import sjtu.ipads.wtune.common.utils.IOSupport;
import sjtu.ipads.wtune.superopt.constraint.ConstraintSupport;
import sjtu.ipads.wtune.superopt.fragment.Fragment;
import sjtu.ipads.wtune.superopt.fragment.FragmentSupport;
import sjtu.ipads.wtune.superopt.fragment.SymbolNaming;
import sjtu.ipads.wtune.superopt.substitution.Substitution;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static java.lang.Integer.parseInt;
import static sjtu.ipads.wtune.common.utils.ListSupport.map;
import static sjtu.ipads.wtune.superopt.constraint.ConstraintSupport.enumConstraints;

public class EnumRule implements Runner {
  private Path success, failure, err, checkpoint;
  private Path prevFailure, prevCheckpoint;
  private final Lock outLock = new ReentrantLock();
  private final Lock errLock = new ReentrantLock();

  private boolean echo;
  private long timeout;
  private int parallelism;
  private int iBegin, jBegin;
  private int numWorker, workerIndex;
  private ExecutorService threadPool;
  private Pair<Fragment, Fragment> target;

  private final AtomicInteger numSkipped = new AtomicInteger(0);

  @Override
  public void prepare(String[] argStrings) throws Exception {
    final Args args = Args.parse(argStrings, 1);
    final String target = args.getOptional("target", String.class, null);
    if (target != null) {
      try {
        final String[] targetFields = target.split("\\|");
        final Fragment source = Fragment.parse(targetFields[0], null);
        final Fragment destination = Fragment.parse(targetFields[1], null);
        this.target = Pair.of(source, destination);
        return;

      } catch (Throwable ex) {
        throw new IllegalArgumentException("invalid target: " + target, ex);
      }
    }

    final String partition = args.getOptional("partition", String.class, "1/0");
    final String[] partitionFields = partition.split("/");
    echo = args.getOptional("echo", boolean.class, false);
    timeout = args.getOptional("timeout", long.class, 240000L);
    parallelism = args.getOptional("parallelism", int.class, 1);

    if (timeout <= 0) throw new IllegalArgumentException("invalid timeout: " + timeout);
    if (parallelism <= 0) throw new IllegalArgumentException("invalid parallelism: " + parallelism);
    if (partitionFields.length != 2)
      throw new IllegalArgumentException("invalid partition: " + partition);
    try {
      numWorker = Integer.parseInt(partitionFields[0]);
      workerIndex = Integer.parseInt(partitionFields[1]);
    } catch (NumberFormatException ex) {
      throw new IllegalArgumentException("invalid partition: " + partition);
    }

    final Path parentDir = Path.of(args.getOptional("dir", String.class, "wtune_data"));
    final String subDirName = LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMddHHmmss"));
    final Path dir = parentDir.resolve("enumerations").resolve("run" + subDirName);

    if (!Files.exists(dir)) Files.createDirectories(dir);

    success = dir.resolve("success");
    failure = dir.resolve("failure");
    err = dir.resolve("err");
    checkpoint = dir.resolve("checkpoint");

    final String prevCheckpointFile = args.getOptional("checkpoint", String.class, null);
    prevCheckpoint = prevCheckpointFile == null ? null : dir.resolve(prevCheckpointFile);

    final String prevFailureFile = args.getOptional("failure", String.class, null);
    prevFailure = prevFailureFile == null ? null : dir.resolve(prevFailureFile);

    final String from = args.getOptional("from", String.class, "0,0");
    final String[] split = from.split(",");
    iBegin = parseInt(split[0]);
    jBegin = parseInt(split[1]);
  }

  @Override
  public void run() throws Exception {
    if (target != null) fromTarget();
    else if (prevFailure != null) fromFailures();
    else fromEnumeration();
  }

  @Override
  public void stop() {}

  private void onShutdown() {
    if (this.threadPool != null) {
      threadPool.shutdown();
      System.out.println(ConstraintSupport.getEnumerationMetric());
      System.out.println("#Skipped=" + numSkipped.get());
    }
  }

  private void openThreadPool() {
    this.threadPool = Executors.newFixedThreadPool(parallelism);
    Runtime.getRuntime().addShutdownHook(new Thread(this::onShutdown));
  }

  private void fromEnumeration() throws IOException {
    final List<Fragment> templates = FragmentSupport.enumFragments();
    final int numTemplates = templates.size();

    int[] completed = null;
    if (prevCheckpoint != null) {
      final List<String> lines = Files.readAllLines(prevCheckpoint);
      completed = new int[lines.size()];
      int i = 0;
      for (String line : lines) {
        final String[] fields = line.split(",");
        final int x = Integer.parseInt(fields[0]);
        final int y = Integer.parseInt(fields[1]);
        completed[i++] = ordinal(numTemplates, x, y);
      }
      Arrays.sort(completed);
      Files.copy(prevCheckpoint, checkpoint);
    }

    final int total = (numTemplates * (numTemplates - 1)) >> 1;
    System.out.printf("Rule Enumeration Begin! Total=%d, Partition=%d\n", total, total / numWorker);

    openThreadPool();
    for (int i = 0; i < numTemplates; ++i) {
      for (int j = i + 1; j < numTemplates; ++j) {
        final int ordinal = ordinal(numTemplates, i, j);
        if (isCompleted(completed, ordinal)) continue;
        if (!isOwned(ordinal)) continue;
        if (i < iBegin || (i == iBegin && j < jBegin)) continue;

        final Fragment f0 = templates.get(i), f1 = templates.get(j);

        if (echo) {
          System.out.printf("%d,%d\n", i, j);
          System.out.println(f0);
          System.out.println(f1);
        }

        final int x = i, y = j;
        threadPool.submit(() -> enumerate(f0, f1, x, y));
      }
    }
  }

  private void fromFailures() throws IOException {
    final List<String> failures = Files.readAllLines(prevFailure);
    final ProgressBar pb = new ProgressBar("Candidates", failures.size());
    openThreadPool();
    for (String failure : failures) {
      pb.step();
      final String[] fields = failure.split("\\|");
      final Fragment f0 = Fragment.parse(fields[0], null);
      final Fragment f1 = Fragment.parse(fields[1], null);
      threadPool.submit(() -> enumerate(f0, f1, -1, -1));
    }
  }

  private void fromTarget() {
    try {
      final SymbolNaming naming = SymbolNaming.mk();
      final Fragment source = target.getLeft();
      final Fragment destination = target.getRight();
      naming.name(source.symbols());
      naming.name(destination.symbols());
      System.out.println(source.stringify(naming));
      System.out.println(destination.stringify(naming));

      final List<Substitution> rules =
          enumConstraints(target.getLeft(), target.getRight(), timeout);

      if (rules == null) {
        System.out.println("==> Skipped.");
        return;
      }

      System.out.printf("==> %d Rules:\n", rules.size());
      for (Substitution rule : rules) System.out.println(rule);
      System.out.println("Metrics: ");
      System.out.println(ConstraintSupport.getEnumerationMetric());

    } catch (Throwable ex) {
      System.out.println("==> Exception!");
      ex.printStackTrace(System.out);
    }
  }

  private int ordinal(int total, int i, int j) {
    assert i < j;
    return ((((total << 1) - i - 1) * i) >> 1) + j - i - 1;
  }

  private boolean isCompleted(int[] completed, int ordinal) {
    return completed != null && Arrays.binarySearch(completed, ordinal) >= 0;
  }

  private boolean isOwned(int ordinal) {
    return ordinal % numWorker == workerIndex;
  }

  private void enumerate(Fragment f0, Fragment f1, int i, int j) {
    boolean outLocked = false, errLocked = false;
    try {
      final List<Substitution> rules = enumConstraints(f0, f1, timeout);
      if (rules == null) {
        numSkipped.incrementAndGet();
        return;
      }

      final List<String> serializedRules = map(rules, Substitution::toString);

      outLock.lock();
      outLocked = true;

      System.out.println("Current Metrics ==>");
      System.out.println(ConstraintSupport.getEnumerationMetric());
      System.out.println("<==");

      IOSupport.appendTo(success, out -> serializedRules.forEach(out::println));
      if (i >= 0 && j >= 1) IOSupport.appendTo(checkpoint, out -> out.printf("%d,%d\n", i, j));

    } catch (Throwable ex) {
      errLock.lock();
      errLocked = true;

      IOSupport.appendTo(
          err,
          err -> {
            err.print(f0);
            err.print('|');
            err.println(f1);
            ex.printStackTrace(err);
            err.println("====");
          });

      IOSupport.appendTo(
          failure,
          err -> {
            err.print(f0);
            err.print('|');
            err.println(f1);
          });

    } finally {
      if (outLocked) outLock.unlock();
      if (errLocked) errLock.unlock();
    }
  }
}
