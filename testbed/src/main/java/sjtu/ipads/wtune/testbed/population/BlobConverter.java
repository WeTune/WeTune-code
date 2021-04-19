package sjtu.ipads.wtune.testbed.population;

import static sjtu.ipads.wtune.testbed.util.RandomHelper.uniformRandomInt;

import java.io.ByteArrayInputStream;
import sjtu.ipads.wtune.sqlparser.ast.SQLDataType;
import sjtu.ipads.wtune.sqlparser.ast.constants.Category;

class BlobConverter implements Converter {
  private static final int NUM_BYTES = 128;

  BlobConverter(SQLDataType dataType) {
    assert dataType.category() == Category.BLOB;
  }

  @Override
  public void convert(int seed, Actuator actuator) {
    final byte[] bytes = new byte[NUM_BYTES];
    for (int i = 0; i < bytes.length; i++) bytes[i] = (byte) uniformRandomInt(seed + i);

    actuator.appendBlob(new ByteArrayInputStream(bytes));
  }
}
