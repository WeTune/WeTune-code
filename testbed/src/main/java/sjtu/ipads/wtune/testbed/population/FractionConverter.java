package sjtu.ipads.wtune.testbed.population;

import static sjtu.ipads.wtune.sqlparser.ast.constants.DataTypeName.DECIMAL;
import static sjtu.ipads.wtune.sqlparser.ast.constants.DataTypeName.FIXED;
import static sjtu.ipads.wtune.sqlparser.ast.constants.DataTypeName.NUMERIC;

import java.math.BigDecimal;
import java.util.stream.IntStream;
import org.apache.commons.lang3.NotImplementedException;
import sjtu.ipads.wtune.sqlparser.ast.SQLDataType;
import sjtu.ipads.wtune.sqlparser.ast.constants.Category;
import sjtu.ipads.wtune.testbed.common.BatchActuator;
import sjtu.ipads.wtune.testbed.util.MathHelper;

class FractionConverter implements Converter {
  private final int max;
  private boolean isDecimal;

  FractionConverter(SQLDataType dataType) {
    assert dataType.category() == Category.FRACTION;

    final int width = Math.max(2, dataType.width());
    final int precision = Math.max(0, dataType.precision());

    max = MathHelper.pow10(Math.min(9, width - precision));

    switch (dataType.name()) {
      case FIXED:
      case NUMERIC:
      case DECIMAL:
        isDecimal = true;
        break;
      default:
        isDecimal = false;
    }
  }

  @Override
  public void convert(int seed, BatchActuator actuator) {
    if (isDecimal) actuator.appendDecimal(BigDecimal.valueOf(seed % max));
    else actuator.appendFraction(seed % max);
  }

  @Override
  public IntStream locate(Object value) {
    throw new NotImplementedException();
  }
}
