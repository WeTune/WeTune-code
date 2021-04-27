package sjtu.ipads.wtune.testbed.population;

import java.util.stream.IntStream;
import org.apache.commons.lang3.NotImplementedException;
import sjtu.ipads.wtune.sqlparser.ast.SQLDataType;
import sjtu.ipads.wtune.testbed.common.BatchActuator;

class EffectiveEnumConverter implements Converter {
  private final int values;
  private SQLDataType dataType;

  EffectiveEnumConverter(int values, SQLDataType dataType) {
    this.values = values;
    this.dataType = dataType;
  }

  @Override
  public void convert(int seed, BatchActuator actuator) {
    switch (dataType.category()) {
      case INTEGRAL:
        actuator.appendInt(seed % values);
        break;
      case STRING:
        actuator.appendString(String.valueOf(seed % values));
        break;
      default:
        throw new UnsupportedOperationException();
    }
  }

  @Override
  public IntStream locate(Object value) {
    throw new NotImplementedException();
  }
}
