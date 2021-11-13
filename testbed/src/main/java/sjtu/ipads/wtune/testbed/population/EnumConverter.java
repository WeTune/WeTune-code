package sjtu.ipads.wtune.testbed.population;

import org.apache.commons.lang3.NotImplementedException;
import sjtu.ipads.wtune.sqlparser.ast1.SqlDataType;
import sjtu.ipads.wtune.sqlparser.ast1.constants.Category;
import sjtu.ipads.wtune.testbed.common.BatchActuator;

import java.util.List;
import java.util.stream.IntStream;

class EnumConverter implements Converter {
  private final SqlDataType dataType;

  EnumConverter(SqlDataType dataType) {
    assert dataType.category() == Category.ENUM;
    this.dataType = dataType;
  }

  @Override
  public void convert(int seed, BatchActuator actuator) {
    final List<String> values = dataType.valuesList();
    actuator.appendString(values.get(seed % values.size()));
  }

  @Override
  public IntStream locate(Object value) {
    throw new NotImplementedException();
  }
}
