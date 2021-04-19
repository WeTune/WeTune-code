package sjtu.ipads.wtune.testbed.population;

import static sjtu.ipads.wtune.testbed.util.MathHelper.isPow10;
import static sjtu.ipads.wtune.testbed.util.MathHelper.isPow2;

import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import sjtu.ipads.wtune.testbed.util.RandGen;
import sjtu.ipads.wtune.testbed.util.RandomHelper;

public class SQLConfig implements Config {
  private static final int DEFAULT_ROW_COUNT = 10000;
  private static final int DEFAULT_BATCH_SIZE = 100;
  private static final RandGen DEFAULT_RAND_GEN = RandomHelper.makeUniformRand();
  private static final ActuatorFactory DEFAULT_ACTUATOR =
      () -> new EchoActuator(new PrintWriter(System.out));
  private static final ActuatorFactory NOOP_ACTUATOR = NoOpActuator::new;

  private int defaultRowCount = DEFAULT_ROW_COUNT;
  private final TObjectIntMap<String> rowCountMap = new TObjectIntHashMap<>();

  private RandGen defaultRandGen = DEFAULT_RAND_GEN;
  private final Map<String, RandGen> randGenMap = new HashMap<>();

  private int batchSize = DEFAULT_BATCH_SIZE;

  private ActuatorFactory actuatorFactory = DEFAULT_ACTUATOR;
  private Properties dbProperties;

  private boolean showProgressBar = true;

  private static void checkRowCount(int rowCount) {
    if (!isPow2(rowCount) && !isPow10(rowCount))
      throw new IllegalArgumentException(
          "row count should be either power of 2 or power of 10, otherwise the uniqueness cannot be enforced");
  }

  @Override
  public int getUnitCount(String collectionName) {
    if (rowCountMap.containsKey(collectionName)) return rowCountMap.get(collectionName);
    else return defaultRowCount;
  }

  @Override
  public void setUnitCount(String collectionName, int rowCount) {
    checkRowCount(rowCount);
    rowCountMap.put(collectionName, rowCount);
  }

  @Override
  public void setDefaultUnitCount(int defaultRowCount) {
    checkRowCount(defaultRowCount);
    this.defaultRowCount = defaultRowCount;
  }

  @Override
  public RandGen getRandomGen(String collectionName, String elementName) {
    return randGenMap.getOrDefault(collectionName + elementName, defaultRandGen);
  }

  @Override
  public void setRandGen(String collectionName, String elementName, RandGen randGen) {
    randGenMap.put(collectionName + elementName, randGen);
  }

  @Override
  public void setDefaultRandGen(RandGen defaultRandGen) {
    this.defaultRandGen = defaultRandGen;
  }

  @Override
  public ActuatorFactory getActuatorFactory() {
    return actuatorFactory;
  }

  @Override
  public boolean showProgressBar() {
    return showProgressBar;
  }

  @Override
  public void setShowProgressBar(boolean showProgressBar) {
    this.showProgressBar = showProgressBar;
  }

  @Override
  public void setDbProperties(Properties dbProperties) {
    this.dbProperties = dbProperties;
    setDryRun(false);
  }

  @Override
  public void setDryRun(boolean flag) {
    if (flag) actuatorFactory = DEFAULT_ACTUATOR;
    else actuatorFactory = new DbActuatorFactory(dbProperties, batchSize);
  }

  @Override
  public void setEcho(boolean flag) {
    if (flag) actuatorFactory = DEFAULT_ACTUATOR;
    else if (actuatorFactory == DEFAULT_ACTUATOR) actuatorFactory = NOOP_ACTUATOR;
  }

  @Override
  public void setBatchSize(int batchSize) {
    this.batchSize = batchSize;
  }
}
