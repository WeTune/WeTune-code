package sjtu.ipads.wtune.testbed.population;

import sjtu.ipads.wtune.sqlparser.ast1.SqlDataType;
import sjtu.ipads.wtune.sqlparser.ast1.constants.Category;
import sjtu.ipads.wtune.testbed.common.BatchActuator;
import sjtu.ipads.wtune.testbed.util.MathHelper;

import java.util.stream.IntStream;

import static java.lang.Integer.parseInt;

class StringConverter implements Converter {
  private final int width;
  private final boolean isArray;

  StringConverter(SqlDataType dataType) {
    assert dataType.category() == Category.STRING;

    if (dataType.width() < 0) width = 128;
    else width = Math.min(128, dataType.width());

    isArray = dataType.dimensions().length > 0;
  }

  @Override
  public void convert(int seed, BatchActuator actuator) {
    final int width = this.width;
    if (width < 10) seed = seed % MathHelper.pow10(width);

    final StringBuilder builder = new StringBuilder(width);
    builder.append(seed);
    builder.append("-".repeat(Math.max(0, width - builder.length())));

    if (isArray) actuator.appendArray(new String[] {builder.toString()}, "varchar");
    else actuator.appendString(builder.toString());
  }

  @Override
  public IntStream locate(Object value) {
    if (!(value instanceof String))
      throw new IllegalArgumentException("cannot decode non-String value");

    final int i = extractSeed((String) value);
    if (width >= 10) return IntStream.of(i);
    else {
      final int range = MathHelper.pow10(width);
      return IntStream.iterate(i, x -> x > 0, x -> x + range);
    }
  }

  private int extractSeed(String s) {
    int i;
    for (i = 0; i < s.length(); i++) {
      final char c = s.charAt(i);
      if (c > '9' || c < '0') break;
    }
    return parseInt(s.substring(0, i));
  }
}
