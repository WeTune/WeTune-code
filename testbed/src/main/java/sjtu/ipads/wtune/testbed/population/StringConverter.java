package sjtu.ipads.wtune.testbed.population;

import static sjtu.ipads.wtune.testbed.util.RandomHelper.uniformRandomInt;

import sjtu.ipads.wtune.sqlparser.ast.SQLDataType;
import sjtu.ipads.wtune.sqlparser.ast.constants.Category;
import sjtu.ipads.wtune.testbed.util.MathHelper;

class StringConverter implements Converter {
  private final int width;
  private boolean isArray;

  StringConverter(SQLDataType dataType) {
    assert dataType.category() == Category.STRING;

    if (dataType.width() < 0) width = 128;
    else width = Math.min(128, dataType.width());

    isArray = dataType.dimensions().length > 0;
  }

  @Override
  public void convert(int seed, Actuator actuator) {
    final int length = Math.min(128, width);

    final int originalSeed = seed;
    if (length < 10) seed = seed % MathHelper.pow10(length);

    final StringBuilder builder = new StringBuilder(length);
    builder.append(seed);
    for (int i = length - builder.length(); i > 0; --i)
      builder.append((char) ('A' + (uniformRandomInt(originalSeed + i) & 15)));

    if (isArray) actuator.appendArray("varchar", new String[] {builder.toString()});
    else actuator.appendString(builder.toString());
  }
}
