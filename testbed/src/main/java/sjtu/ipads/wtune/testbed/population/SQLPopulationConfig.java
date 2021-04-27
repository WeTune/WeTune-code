package sjtu.ipads.wtune.testbed.population;

import static sjtu.ipads.wtune.testbed.util.MathHelper.isPow10;

import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.function.Function;
import sjtu.ipads.wtune.testbed.common.BatchActuatorFactory;
import sjtu.ipads.wtune.testbed.util.RandGen;
import sjtu.ipads.wtune.testbed.util.RandomHelper;

public class SQLPopulationConfig implements PopulationConfig {
  private static final int DEFAULT_ROW_COUNT = 10000;
  private static final int DEFAULT_BATCH_SIZE = 500;
  private static final RandGen DEFAULT_RAND_GEN = RandomHelper.makeUniformRand();
  private static final BatchActuatorFactory DEFAULT_ACTUATOR =
      ignored -> new EchoActuator(new PrintWriter(System.out));

  private int defaultRowCount = DEFAULT_ROW_COUNT;
  private final TObjectIntMap<String> rowCountMap = new TObjectIntHashMap<>();

  private RandGen defaultRandGen = DEFAULT_RAND_GEN;
  private final Map<String, RandGen> randGenMap = new HashMap<>();

  private BatchActuatorFactory actuatorFactory = DEFAULT_ACTUATOR;
  private Properties dbProperties;
  private Function<String, PrintWriter> dumpWriterFactory = ignored -> new PrintWriter(System.out);

  private int batchSize = DEFAULT_BATCH_SIZE;
  private boolean showProgressBar = true;

  private static void checkRowCount(int rowCount) {
    if (!isPow10(rowCount))
      throw new IllegalArgumentException("row count should be either power of 10");
  }

  @Override
  public void setRandomSeed(int i) {
    RandomHelper.GLOBAL_SEED = i;
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
  public BatchActuatorFactory getActuatorFactory() {
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
    if (flag)
      if (dumpWriterFactory == null)
        actuatorFactory = ignored -> new EchoActuator(new PrintWriter(System.out));
      else
        actuatorFactory = name -> new EchoActuator(new PrintWriter(dumpWriterFactory.apply(name)));
    else actuatorFactory = new BatchActuatorFactoryImpl(dbProperties, batchSize);
  }

  @Override
  public void setDump(Function<String, PrintWriter> factory) {
    this.dumpWriterFactory = factory;
    setDryRun(true);
  }

  @Override
  public void setBatchSize(int batchSize) {
    this.batchSize = batchSize;
  }
}
