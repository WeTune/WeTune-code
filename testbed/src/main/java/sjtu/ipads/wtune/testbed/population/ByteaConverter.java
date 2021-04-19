package sjtu.ipads.wtune.testbed.population;

import sjtu.ipads.wtune.sqlparser.ast.SQLDataType;

class ByteaConverter implements Converter {
  ByteaConverter(SQLDataType dataType) {
    assert dataType.name().equals("bytea");
  }

  @Override
  public void convert(int seed, Actuator actuator) {
    final byte[] bytes = new byte[4];
    bytes[0] = (byte) ((seed & 0xFF000000) >> 24);
    bytes[1] = (byte) ((seed & 0x00FF0000) >> 16);
    bytes[2] = (byte) ((seed & 0x0000FF00) >> 8);
    bytes[3] = (byte) ((seed & 0x000000FF));

    actuator.appendBytes(bytes);
  }
}
