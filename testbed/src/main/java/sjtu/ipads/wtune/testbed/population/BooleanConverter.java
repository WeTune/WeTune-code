package sjtu.ipads.wtune.testbed.population;

import sjtu.ipads.wtune.sqlparser.ast.SQLDataType;
import sjtu.ipads.wtune.sqlparser.ast.constants.Category;

class BooleanConverter implements Converter {
  BooleanConverter(SQLDataType dataType) {
    assert dataType.category() == Category.BOOLEAN
        || (dataType.category() == Category.BIT_STRING && dataType.width() == 1);
  }

  @Override
  public void convert(int seed, Actuator actuator) {
    actuator.appendBool((seed & 1) == 0);
  }
}
