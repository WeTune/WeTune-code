package sjtu.ipads.wtune.testbed.population;

import java.io.PrintWriter;
import java.util.Properties;
import java.util.function.Function;
import java.util.function.Supplier;
import sjtu.ipads.wtune.testbed.common.BatchActuatorFactory;
import sjtu.ipads.wtune.testbed.util.RandGen;

public interface PopulationConfig {
  int randomSeed();

  int unitCountOf(String collectionName);

  RandGen randomGenOf(String collectionName, String elementName);

  BatchActuatorFactory actuatorFactory();

  boolean showProgressBar();

  boolean needPrePopulation();

  void setRandomSeed(int i);

  void setDefaultUnitCount(int rowCount);

  void setUnitCount(String collectionName, int rowCount);

  void setDefaultRandGen(Supplier<RandGen> randGen);

  void setRandGen(String collectionName, String elementName, RandGen randGen);

  void setDbProperties(Properties properties);

  void setBatchSize(int batchSize);

  void setShowProgressBar(boolean flag);

  void setDump(Function<String, PrintWriter> factory);

  void setNeedPrePopulation(boolean flag);

  static PopulationConfig make() {
    return new SQLPopulationConfig();
  }
}
