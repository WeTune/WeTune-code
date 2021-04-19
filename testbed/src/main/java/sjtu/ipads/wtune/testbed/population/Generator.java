package sjtu.ipads.wtune.testbed.population;

import java.util.Arrays;

public interface Generator {
  void generate(int seed, Actuator actuator);

}
