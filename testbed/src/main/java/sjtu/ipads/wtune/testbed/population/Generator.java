package sjtu.ipads.wtune.testbed.population;

import java.util.stream.IntStream;
import sjtu.ipads.wtune.testbed.common.BatchActuator;

public interface Generator {
  void generate(int seed, BatchActuator actuator);

  default Object generate(int seed) {
    final CollectActuator collector = new CollectActuator();
    generate(seed, collector);
    return collector.obj();
  }

  IntStream locate(Object value);
}
