package sjtu.ipads.wtune.superopt.runner;

import me.tongfei.progressbar.ProgressBar;
import sjtu.ipads.wtune.common.utils.Args;
import sjtu.ipads.wtune.superopt.constraint.ConstraintSupport;
import sjtu.ipads.wtune.superopt.fragment.Fragment;
import sjtu.ipads.wtune.superopt.fragment.FragmentSupport;

import java.security.interfaces.RSAMultiPrimePrivateCrtKey;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

import static sjtu.ipads.wtune.common.utils.ListSupport.map;
import static sjtu.ipads.wtune.superopt.constraint.ConstraintSupport.*;

public class DryRun implements Runner {
  private long timeout;
  private int parallelism;
  private int samples;
  private ExecutorService threadPool;
  private ProgressBar progressBar;
  private CountDownLatch latch;

  @Override
  public void prepare(String[] argStrings) throws Exception {
    final Args args = Args.parse(argStrings, 1);
    timeout = args.getOptional("timeout", long.class, 240000L);
    parallelism = args.getOptional("parallelism", int.class, 1);
    samples = args.getOptional("samples", int.class, -1);
    if (timeout <= 0) throw new IllegalArgumentException("invalid timeout: " + timeout);
    if (parallelism <= 0) throw new IllegalArgumentException("invalid parallelism: " + parallelism);
  }

  @Override
  public void run() throws Exception {
    if (samples < 0) runAll();
    else runSample();
  }

  @Override
  public void stop() {
    System.out.println(ConstraintSupport.getEnumerationMetric());
  }

  private void runAll() {
    final List<Fragment> templates = FragmentSupport.enumFragments();
    final int numTemplates = templates.size();
    final int totalPairs = (numTemplates * (numTemplates + 1)) >> 1;

    latch = new CountDownLatch(totalPairs);
    threadPool = Executors.newFixedThreadPool(parallelism);

    try (final ProgressBar pb = new ProgressBar("Samples", totalPairs)) {
      progressBar = pb;

      for (int i = 0; i < numTemplates; ++i) {
        for (int j = i; j < numTemplates; ++j) {
          final Fragment f0 = templates.get(i), f1 = templates.get(j);
          threadPool.submit(() -> enumerate(f0, f1));
        }
      }

      latch.await();
      threadPool.shutdown();

    } catch (InterruptedException ignored) {
    }
  }

  private void runSample() {
    final List<Fragment> templates = FragmentSupport.enumFragments();
    final int numTemplates = templates.size();
    final int totalPairs = (numTemplates * (numTemplates + 1)) >> 1;
    final int samples = Integer.min(totalPairs, this.samples);

    latch = new CountDownLatch(samples);
    threadPool = Executors.newFixedThreadPool(parallelism);

    try (final ProgressBar pb = new ProgressBar("Samples", totalPairs)) {
      progressBar = pb;

      final ThreadLocalRandom r = ThreadLocalRandom.current();
      for (int n = 0; n < samples; ++n) {
        final int x = r.nextInt(totalPairs);
        final int y = r.nextInt(totalPairs);
        final int i = Integer.min(x, y);
        final int j = Integer.max(x, y);
        final Fragment f0 = templates.get(i), f1 = templates.get(j);
        threadPool.submit(() -> enumerate(f0, f1));
      }

      latch.await();
      threadPool.shutdown();

    } catch (InterruptedException ignored) {
    }
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

    enumConstraints(
        f0,
        f1,
        timeout,
        ENUM_FLAG_DRY_RUN
            | ENUM_FLAG_DISABLE_BREAKER_0
            | ENUM_FLAG_DISABLE_BREAKER_1
            | ENUM_FLAG_DISABLE_BREAKER_2,
        null);
  }
}
