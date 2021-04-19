package sjtu.ipads.wtune.testbed.population;

import static sjtu.ipads.wtune.testbed.util.RandomHelper.uniformRandomInt;

import sjtu.ipads.wtune.sqlparser.ast.SQLDataType;
import sjtu.ipads.wtune.sqlparser.ast.constants.Category;

class BitStringConverter implements Converter {
  private final SQLDataType dataType;

  BitStringConverter(SQLDataType dataType) {
    assert dataType.category() == Category.BIT_STRING;
    this.dataType = dataType;
  }

  @Override
  public void convert(int seed, Actuator actuator) {
    final int length = Math.max(1, dataType.width());

    final StringBuilder builder = new StringBuilder(length);
    for (int i = 0; i < length; i++) builder.append((char) ('0' + uniformRandomInt(seed + i) & 1));

    actuator.appendString(builder.toString());
  }
}
