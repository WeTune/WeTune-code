package sjtu.ipads.wtune.testbed.profile;

import static java.util.Objects.requireNonNull;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Properties;
import java.util.function.Function;
import sjtu.ipads.wtune.stmt.Statement;
import sjtu.ipads.wtune.testbed.population.Generators;

public interface ProfileConfig {
  int warmupCycles();

  int profileCycles();

  int randomSeed();

  boolean dryRun();

  Generators generators();

  ExecutorFactory executorFactory();

  ObjectOutputStream savedParamOut(Statement stmt);

  ObjectInputStream saveParamIn(Statement stmt);

  void setWarmupCycles(int x);

  void setProfileCycles(int x);

  void setRandomSeed(int x);

  void setDryRun(boolean dryRun);

  void setGenerators(Generators generators);

  void setDbProperties(Properties properties);

  void setParamSaveFile(Function<Statement, String> mapFunc);

  static ProfileConfig make(Generators generators) {
    return new ProfileConfigImpl(requireNonNull(generators));
  }
}
