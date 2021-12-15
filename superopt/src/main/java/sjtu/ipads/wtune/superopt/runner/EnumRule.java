package sjtu.ipads.wtune.superopt.runner;

import me.tongfei.progressbar.ProgressBar;
import sjtu.ipads.wtune.common.utils.IOSupport;
import sjtu.ipads.wtune.superopt.fragment.Fragment;
import sjtu.ipads.wtune.superopt.fragment.FragmentSupport;
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

import static java.lang.Integer.parseInt;
import static sjtu.ipads.wtune.common.utils.ListSupport.map;
import static sjtu.ipads.wtune.superopt.constraint.ConstraintSupport.enumConstraints;

public class EnumRule implements Runner {
  private Path success, failure, checkpoint;
  private Path prevFailure, prevCheckpoint;
  private Path err;
  private boolean echo;
  private long timeout;
  private int parallelism;
  private int iBegin, jBegin;
  private int numWorker, workerIndex;
  private ExecutorService threadPool;

  @Override
  public void prepare(String[] argStrings) throws Exception {
    final Args args = Args.parse(argStrings, 1);
    echo = args.getOptional("echo", boolean.class, false);
    timeout = args.getOptional("timeout", long.class, 120000L);
    parallelism = args.getOptional("parallelism", int.class, 1);
    numWorker = args.getPositional(0, int.class);
    workerIndex = args.getPositional(1, int.class);

    final Path parentDir = Path.of(args.getOptional("dir", String.class, "wtune_data"));
    final String subDirName = LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMddHHmmss"));
    final Path dir = parentDir.resolve("rule" + subDirName);

    if (!Files.exists(dir)) Files.createDirectories(dir);

    success = dir.resolve("success");
    failure = dir.resolve("failure");
    err = dir.resolve("err");
    checkpoint = dir.resolve("checkpoint");

    final String prevCheckpointFile = args.getOptional("checkpoint", String.class, null);
    prevCheckpoint = prevCheckpointFile == null ? null : Path.of(prevCheckpointFile);

    final String prevFailureFile = args.getOptional("failure", String.class, null);
    prevFailure = prevFailureFile == null ? null : Path.of(prevFailureFile);

    final String from = args.getOptional("from", String.class, "0,0");
    final String[] split = from.split(",");
    iBegin = parseInt(split[0]);
    jBegin = parseInt(split[1]);
  }

  @Override
  public void run() throws Exception {
    if (prevFailure != null) fromFailures();
    else fromEnumeration();
  }

  @Override
  public void stop() {
    if (threadPool != null) threadPool.shutdown();
  }

  private void openThreadPool() {
    final ExecutorService threadPool = Executors.newFixedThreadPool(parallelism);
    this.threadPool = threadPool;
    Runtime.getRuntime().addShutdownHook(new Thread(threadPool::shutdown));
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
    }

    final int total = (numTemplates * (numTemplates - 1)) >> 1;
    final ProgressBar pb = new ProgressBar("Candidates", total);

    openThreadPool();
    for (int i = 0; i < numTemplates; ++i) {
      for (int j = i + 1; j < numTemplates; ++j) {
        pb.step();

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

  private void fromString(String fragmentPair) {
    final String[] split = fragmentPair.split("\\|");
    final Fragment f0 = Fragment.parse(split[0], null);
    final Fragment f1 = Fragment.parse(split[1], null);
    enumerate(f0, f1, -1, -1);
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
    try {
      final List<Substitution> rules = enumConstraints(f0, f1, timeout);
      final List<String> serializedRules = map(rules, Substitution::toString);

      IOSupport.printWithLock(success, out -> serializedRules.forEach(out::println));
      if (i >= 0 && j >= 1) IOSupport.printWithLock(checkpoint, out -> out.printf("%d,%d\n", i, j));

    } catch (Throwable ex) {
      IOSupport.printWithLock(
          err,
          err -> {
            err.print(f0);
            err.println('|');
            err.print(f1);
            ex.printStackTrace(err);
            err.println("====");
          });

      IOSupport.printWithLock(
          failure,
          err -> {
            err.print(f0);
            err.print('|');
            err.println(f1);
          });
    }
  }
}
