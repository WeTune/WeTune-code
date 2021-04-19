package sjtu.ipads.wtune.testbed.population;

import java.util.Properties;
import sjtu.ipads.wtune.testbed.util.RandGen;

public interface Config {
  int getUnitCount(String collectionName);

  void setUnitCount(String collectionName, int rowCount);

  void setDefaultUnitCount(int rowCount);

  RandGen getRandomGen(String collectionName, String elementName);

  void setRandGen(String collectionName, String elementName, RandGen randGen);

  void setDefaultRandGen(RandGen randGen);

  ActuatorFactory getActuatorFactory();

  // this will call setDryRun(false)
  void setDbProperties(Properties properties);

  void setDryRun(boolean flag);

  void setEcho(boolean flag);

  void setBatchSize(int batchSize);

  boolean showProgressBar();

  void setShowProgressBar(boolean flag);
}
