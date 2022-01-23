package sjtu.ipads.wtune.superopt.runner;

import me.tongfei.progressbar.ProgressBar;
import sjtu.ipads.wtune.common.utils.Args;
import sjtu.ipads.wtune.superopt.constraint.ConstraintSupport;
import sjtu.ipads.wtune.superopt.fragment.Fragment;
import sjtu.ipads.wtune.superopt.fragment.FragmentSupport;
import sjtu.ipads.wtune.superopt.fragment.Symbols;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static sjtu.ipads.wtune.common.utils.ListSupport.map;
import static sjtu.ipads.wtune.superopt.constraint.ConstraintSupport.ENUM_FLAG_DRY_RUN;
import static sjtu.ipads.wtune.superopt.constraint.ConstraintSupport.enumConstraints;
import static sjtu.ipads.wtune.superopt.fragment.Symbol.Kind.*;
import static sjtu.ipads.wtune.superopt.fragment.Symbol.Kind.PRED;

public class PrintDryRunStatistic implements Runner {
  private long timeout;
  private int parallelism;
  private ExecutorService threadPool;
  private ProgressBar progressBar;
  private CountDownLatch latch;

  private final AtomicInteger numSkipped = new AtomicInteger(0);

  @Override
  public void prepare(String[] argStrings) throws Exception {
    final Args args = Args.parse(argStrings, 1);
    timeout = args.getOptional("timeout", long.class, 240000L);
    parallelism = args.getOptional("parallelism", int.class, 1);
    if (timeout <= 0) throw new IllegalArgumentException("invalid timeout: " + timeout);
    if (parallelism <= 0) throw new IllegalArgumentException("invalid parallelism: " + parallelism);
  }

  @Override
  public void run() throws Exception {
    final List<Fragment> templates = FragmentSupport.enumFragments();
    final int numTemplates = templates.size();

    int[] completed = null;

    final int totalPairs = (numTemplates * (numTemplates + 1)) >> 1;

    latch = new CountDownLatch(totalPairs);
    threadPool = Executors.newFixedThreadPool(parallelism);

    try (final ProgressBar pb = new ProgressBar("Candidates", totalPairs)) {
      progressBar = pb;

      for (int i = 0; i < numTemplates; ++i) {
        for (int j = i; j < numTemplates; ++j) {
          final Fragment f0 = templates.get(i), f1 = templates.get(j);
          threadPool.submit(() -> enumerate(f0, f1));
        }
      }

      latch.await();
      threadPool.shutdown();
    }
  }

  @Override
  public void stop() {
    System.out.println(ConstraintSupport.getEnumerationMetric());
    System.out.println("#Skipped=" + numSkipped.get());
  }

  private void enumerate(Fragment f0_, Fragment f1_) {
    enumerate0(f0_, f1_);
    if (progressBar != null) progressBar.step();
    if (latch != null) latch.countDown();
  }

  private void enumerate0(Fragment f0_, Fragment f1_) {
    final Fragment f0 = f0_;
    final Fragment f1;
    if (f0_ != f1_) f1 = f1_;
    else {
      f1 = f1_.copy();
      FragmentSupport.setupFragment(f1);
    }

    enumConstraints(f0, f1, timeout, ENUM_FLAG_DRY_RUN, null);
  }
}
