package sjtu.ipads.wtune.testbed.profile;

import java.util.Properties;
import sjtu.ipads.wtune.testbed.population.Generators;

class ProfileConfigImpl implements ProfileConfig {
  private int warmupCycles, profileCycles;
  private int randomSeed;
  private Generators generators;
  private ExecutorFactory factory;

  ProfileConfigImpl(Generators generators) {
    this.warmupCycles = 100;
    this.profileCycles = 100;
    this.randomSeed = 0x98761234;
    this.generators = generators;
    this.factory = ignored -> new NoOpExecutor();
  }

  @Override
  public int warmupCycles() {
    return warmupCycles;
  }

  @Override
  public int profileCycles() {
    return profileCycles;
  }

  @Override
  public int randomSeed() {
    return randomSeed;
  }

  @Override
  public Generators generators() {
    return generators;
  }

  @Override
  public ExecutorFactory executorFactory() {
    return factory;
  }

  @Override
  public void setWarmupCycles(int x) {
    this.warmupCycles = x;
  }

  @Override
  public void setProfileCycles(int x) {
    this.profileCycles = x;
  }

  @Override
  public void setRandomSeed(int x) {
    this.randomSeed = x;
  }

  @Override
  public void setGenerators(Generators generators) {
    this.generators = generators;
  }

  @Override
  public void setDbProperties(Properties properties) {
    if (properties == null) this.factory = ignored -> new NoOpExecutor();
    else this.factory = new ExecutorFactoryImpl(properties);
  }
}
