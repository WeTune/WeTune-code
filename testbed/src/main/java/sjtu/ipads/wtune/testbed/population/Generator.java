package sjtu.ipads.wtune.testbed.population;

import sjtu.ipads.wtune.testbed.common.BatchActuator;

import java.util.stream.IntStream;

public interface Generator {
  void generate(int seed, BatchActuator actuator);

  IntStream locate(Object value);

  default Object generate(int seed) {
    final CollectActuator collector = new CollectActuator();
    generate(seed, collector);
    return collector.obj();
  }

  default boolean isPrePopulated() {
    return true;
  }
}
