package sjtu.ipads.wtune.testbed.profile;

import static java.util.Objects.requireNonNull;

import java.util.Properties;
import sjtu.ipads.wtune.testbed.population.Generators;

public interface ProfileConfig {
  int warmupCycles();

  int profileCycles();

  int randomSeed();

  Generators generators();

  ExecutorFactory executorFactory();

  void setWarmupCycles(int x);

  void setProfileCycles(int x);

  void setRandomSeed(int x);

  void setGenerators(Generators generators);

  void setDbProperties(Properties properties);

  static ProfileConfig make(Generators generators) {
    return new ProfileConfigImpl(requireNonNull(generators));
  }
}
