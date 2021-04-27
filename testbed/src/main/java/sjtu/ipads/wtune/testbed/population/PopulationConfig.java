package sjtu.ipads.wtune.testbed.population;

import java.io.PrintWriter;
import java.util.Properties;
import java.util.function.Function;
import sjtu.ipads.wtune.testbed.common.BatchActuatorFactory;
import sjtu.ipads.wtune.testbed.util.RandGen;

public interface PopulationConfig {
  void setRandomSeed(int i);

  int getUnitCount(String collectionName);

  void setUnitCount(String collectionName, int rowCount);

  void setDefaultUnitCount(int rowCount);

  RandGen getRandomGen(String collectionName, String elementName);

  void setRandGen(String collectionName, String elementName, RandGen randGen);

  void setDefaultRandGen(RandGen randGen);

  BatchActuatorFactory getActuatorFactory();

  // this will call setDryRun(false)
  void setDbProperties(Properties properties);

  void setDryRun(boolean flag);

  void setBatchSize(int batchSize);

  boolean showProgressBar();

  void setShowProgressBar(boolean flag);

  void setDump(Function<String, PrintWriter> factory);

  static PopulationConfig make() {
    return new SQLPopulationConfig();
  }
}
