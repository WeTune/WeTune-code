package sjtu.ipads.wtune.testbed.population;

import java.util.stream.IntStream;
import org.apache.commons.lang3.NotImplementedException;
import sjtu.ipads.wtune.sqlparser.ast.SQLDataType;
import sjtu.ipads.wtune.sqlparser.ast.constants.Category;
import sjtu.ipads.wtune.testbed.common.BatchActuator;

class BooleanConverter implements Converter {
  BooleanConverter(SQLDataType dataType) {
    assert dataType.category() == Category.BOOLEAN
        || (dataType.category() == Category.BIT_STRING && dataType.width() == 1);
  }

  @Override
  public void convert(int seed, BatchActuator actuator) {
    actuator.appendBool((seed & 1) == 0);
  }

  @Override
  public IntStream locate(Object value) {
    throw new NotImplementedException();
  }
}
