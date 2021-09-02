package sjtu.ipads.wtune.testbed.profile;

import static java.lang.Math.abs;
import static sjtu.ipads.wtune.testbed.util.RandomHelper.uniformRandomInt;

import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.System.Logger.Level;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import sjtu.ipads.wtune.stmt.Statement;
import sjtu.ipads.wtune.stmt.resolver.ParamDesc;
import sjtu.ipads.wtune.stmt.resolver.Params;

class ProfilerImpl implements Profiler {
  private final Statement statement;
  private final ProfileConfig config;
  private final ParamsGen paramsGen;
  private final Executor exectuor;
  private final Metric metric;

  private boolean recording;
  private long lastElapsed;

  private int warmupCycles;
  private int profileCycles;

  private TIntList seeds;
  private List<Map<ParamDesc, Object>> params;

  ProfilerImpl(Statement stmt, ProfileConfig config) {
    this.statement = stmt;
    this.config = config;

    this.paramsGen = ParamsGen.make(stmt.parsed().manager(Params.class), config.generators());
    this.exectuor = config.executorFactory().make(stmt.parsed().toString());
    this.metric = Metric.make(config.profileCycles());

    this.warmupCycles = config.warmupCycles();
    this.profileCycles = config.profileCycles();
  }

  @Override
  public Statement statement() {
    return statement;
  }

  @Override
  public TIntList seeds() {
    return seeds;
  }

  @Override
  public Metric metric() {
    return metric;
  }

  @Override
  public ParamsGen paramsGen() {
    return paramsGen;
  }

  @Override
  public void setSeeds(TIntList seeds) {
    this.seeds = seeds;
  }

  @Override
  public boolean prepare() {
    final int paramsCount = config.profileCycles();
    final List<Map<ParamDesc, Object>> params = new ArrayList<>(paramsCount);

    TIntList seeds = this.seeds;
    if (seeds == null) {
      seeds = new TIntArrayList(paramsCount);

      int seed = abs(uniformRandomInt(config.randomSeed()));
      for (int i = 0; i < paramsCount; ++i) {
        seed = ParamsGen.setEligibleSeed(paramsGen, seed);
        if (seed >= 0)
          if (paramsGen.generateAll()) { // pivot seed has been set
            seeds.add(seed);
            params.add(paramsGen.values());
          } else {
            LOG.log(Level.ERROR, "cannot generate value at seed {0}", seed);
            return false;
          }
        else break;
      }

      if (seeds.isEmpty()) {
        LOG.log(Level.ERROR, "cannot find any eligible seed");
        return false;
      }
    } else {
      for (int i = 0; i < seeds.size(); ++i)
        if (paramsGen.setPivotSeed(seeds.get(i)))
          if (paramsGen.generateAll()) params.add(paramsGen.values());
          else {
            LOG.log(Level.ERROR, "cannot set seed {0}", seeds.get(i));
            return false;
          }
        else {
          LOG.log(Level.ERROR, "cannot generate value at seed {0}", seeds.get(i));
          return false;
        }
    }

    this.seeds = seeds;
    this.params = params;
    return true;
  }

  @Override
  public boolean run() {
    if (config.dryRun()) return true;

    // probe run
    recording = false;
    if (!run0(0)) return false;

    adjustNumCycles(); // for those long-running ones (e.g. > 5s), needn't to repeatedly run

    recording = false;
    for (int i = 0, bound = warmupCycles; i < bound; ++i)
      if (!run0(i)) {
        return false;
      }

    recording = true;
    for (int i = 0, bound = profileCycles; i < bound; ++i)
      if (!run0(i)) {
        return false;
      }

    return true;
  }

  @Override
  public void close() {
    exectuor.close();
  }

  @Override
  public void saveParams(ObjectOutputStream stream) throws IOException {
    if (stream == null) return;
    stream.writeInt(config.randomSeed());
    stream.writeInt(config.generators().config().randomSeed());
    stream.writeObject(params);
  }

  @Override
  public boolean readParams(ObjectInputStream stream) throws IOException, ClassNotFoundException {
    if (stream == null) return false;
    final int profileSeed = stream.readInt();
    final int populationSeed = stream.readInt();
    if (populationSeed != config.generators().config().randomSeed()
        || profileSeed != config.randomSeed()) return false;

    this.params = (List<Map<ParamDesc, Object>>) stream.readObject();
    return true;
  }

  private boolean run0(int cycle) {
    final Map<ParamDesc, Object> params = this.params.get(cycle % this.params.size());
    if (!exectuor.installParams(params)) return false;

    final long elapsed = exectuor.execute();
    if (elapsed < 0) return false;
    exectuor.endOne();

    if (recording) metric.addRecord(elapsed);

    lastElapsed = elapsed;

    return true;
  }

  private void adjustNumCycles() {
    if (lastElapsed >= 5_000_000_000L) { // 10 seconds
      warmupCycles = 0;
      profileCycles = 0;
      metric.addRecord(lastElapsed);
      return;
    }

    final int cycleBudget = (int) (10_000_000_000L / lastElapsed);
    if (cycleBudget <= profileCycles) {
      warmupCycles = 0;
      profileCycles = cycleBudget;
    } else {
      warmupCycles = Math.min(warmupCycles, cycleBudget - profileCycles);
    }
    if (warmupCycles > 0) warmupCycles -= 1;
    if (profileCycles > 0) profileCycles -= 1;
  }
}
