package sjtu.ipads.wtune.testbed.population;

import org.apache.commons.lang3.NotImplementedException;
import sjtu.ipads.wtune.sqlparser.ast1.SqlDataType;
import sjtu.ipads.wtune.testbed.common.BatchActuator;

import java.util.stream.IntStream;

class EffectiveEnumConverter implements Converter {
  private final int values;
  private final SqlDataType dataType;

  EffectiveEnumConverter(int values, SqlDataType dataType) {
    this.values = values;
    this.dataType = dataType;
  }

  @Override
  public void convert(int seed, BatchActuator actuator) {
    switch (dataType.category()) {
      case INTEGRAL -> actuator.appendInt(seed % values);
      case STRING -> actuator.appendString(String.valueOf(seed % values));
      default -> throw new UnsupportedOperationException();
    }
  }

  @Override
  public IntStream locate(Object value) {
    throw new NotImplementedException();
  }
}
