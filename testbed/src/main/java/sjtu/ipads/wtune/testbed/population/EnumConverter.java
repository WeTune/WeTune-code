package sjtu.ipads.wtune.testbed.population;

import java.util.List;
import sjtu.ipads.wtune.sqlparser.ast.SQLDataType;
import sjtu.ipads.wtune.sqlparser.ast.constants.Category;

class EnumConverter implements Converter {
  private final SQLDataType dataType;

  EnumConverter(SQLDataType dataType) {
    assert dataType.category() == Category.ENUM;
    this.dataType = dataType;
  }

  @Override
  public void convert(int seed, Actuator actuator) {
    final List<String> values = dataType.valuesList();
    actuator.appendString(values.get(seed % values.size()));
  }
}
