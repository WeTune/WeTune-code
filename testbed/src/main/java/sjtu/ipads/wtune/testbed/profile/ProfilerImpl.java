package sjtu.ipads.wtune.testbed.profile;

import static java.lang.Math.abs;
import static sjtu.ipads.wtune.testbed.util.RandomHelper.uniformRandomInt;

import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
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
  private final Executor actuator;
  private final Metric metric;

  private boolean recording;
  private TIntList seeds;
  private List<Map<ParamDesc, Object>> params;

  ProfilerImpl(Statement stmt, ProfileConfig config) {
    this.statement = stmt;
    this.config = config;
    this.paramsGen = ParamsGen.make(stmt.parsed().manager(Params.class), config.generators());
    this.actuator = config.executorFactory().make(stmt.parsed().toString());
    this.metric = Metric.make(config.profileCycles());
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
    if (seeds != null) {
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

    } else {
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
    }

    this.seeds = seeds;
    this.params = params;
    return true;
  }

  @Override
  public boolean run() {
    recording = false;
    for (int i = 0, bound = config.warmupCycles(); i < bound; ++i)
      if (!run0(i)) {
        return false;
      }

    recording = true;
    for (int i = 0, bound = config.profileCycles(); i < bound; ++i)
      if (!run0(i)) {
        return false;
      }

    return true;
  }

  @Override
  public void close() {
    actuator.close();
  }

  private boolean run0(int cycle) {
    final Map<ParamDesc, Object> params = this.params.get(cycle % this.params.size());
    if (!actuator.installParams(params)) return false;

    final long start = System.nanoTime();
    if (!actuator.execute()) return false;
    final long end = System.nanoTime();
    actuator.endOne();

    if (recording) metric.addRecord(end - start);

    return true;
  }
}
