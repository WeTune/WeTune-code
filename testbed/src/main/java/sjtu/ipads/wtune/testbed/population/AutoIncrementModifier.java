package sjtu.ipads.wtune.testbed.population;

import java.util.stream.IntStream;
import sjtu.ipads.wtune.testbed.common.BatchActuator;

class AutoIncrementModifier implements Modifier {
  AutoIncrementModifier() {}

  @Override
  public void modify(int seed, BatchActuator actuator) {
    // `seed` here should be a row number
    // for auto-increment column, the value at the i-th row is just i+1
    actuator.appendInt(seed + 1); // row number is 0-based, plus 1
  }

  @Override
  public IntStream locate(Object value) {
    if (!(value instanceof Integer))
      throw new IllegalArgumentException("cannot locate non-integer value");

    return IntStream.of(((Integer) value) - 1);
  }
}
