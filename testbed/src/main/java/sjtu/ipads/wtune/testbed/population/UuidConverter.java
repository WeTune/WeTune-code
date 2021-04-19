package sjtu.ipads.wtune.testbed.population;

import java.sql.Types;
import java.util.UUID;
import sjtu.ipads.wtune.sqlparser.ast.SQLDataType;
import sjtu.ipads.wtune.sqlparser.ast.constants.Category;

class UuidConverter implements Converter {
  UuidConverter(SQLDataType dataType) {
    assert dataType.category() == Category.UUID;
  }

  @Override
  public void convert(int seed, Actuator actuator) {
    actuator.appendObject(UUID.randomUUID(), Types.OTHER);
  }
}
