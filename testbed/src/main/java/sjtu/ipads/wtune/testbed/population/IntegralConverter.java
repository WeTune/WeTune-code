package sjtu.ipads.wtune.testbed.population;

import sjtu.ipads.wtune.sqlparser.ast.SQLDataType;
import sjtu.ipads.wtune.sqlparser.ast.constants.Category;

public class IntegralConverter implements Converter {
  private final String typeName;
  private final int max;
  private final boolean isArray;

  public IntegralConverter(SQLDataType dataType) {
    assert dataType.category() == Category.INTEGRAL;

    typeName = dataType.name();
    isArray = dataType.dimensions().length > 0;

    final int bytes = Math.min(4, dataType.storageSize());
    max = ~(Integer.MIN_VALUE >> (32 - (bytes << 3)));
  }

  @Override
  public void convert(int seed, Actuator actuator) {
    final int value = seed % max;
    if (isArray) actuator.appendArray(typeName, new Integer[] {value});
    else actuator.appendInt(value);
  }
}
