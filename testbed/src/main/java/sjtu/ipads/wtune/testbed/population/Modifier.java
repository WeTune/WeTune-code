package sjtu.ipads.wtune.testbed.population;

import sjtu.ipads.wtune.testbed.common.BatchActuator;

public interface Modifier extends Generator {
  void modify(int seed, BatchActuator actuator);

  @Override
  default void generate(int seed, BatchActuator actuator) {
    modify(seed, actuator);
  }
}
