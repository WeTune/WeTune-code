package sjtu.ipads.wtune.testbed.population;

import java.util.List;
import java.util.stream.IntStream;
import org.apache.commons.lang3.NotImplementedException;
import sjtu.ipads.wtune.sqlparser.ast.SQLDataType;
import sjtu.ipads.wtune.sqlparser.ast.constants.Category;
import sjtu.ipads.wtune.testbed.common.BatchActuator;

class EnumConverter implements Converter {
  private final SQLDataType dataType;

  EnumConverter(SQLDataType dataType) {
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
