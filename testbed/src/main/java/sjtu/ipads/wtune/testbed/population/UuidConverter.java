package sjtu.ipads.wtune.testbed.population;

import java.sql.Types;
import java.util.UUID;
import java.util.stream.IntStream;
import org.apache.commons.lang3.NotImplementedException;
import sjtu.ipads.wtune.sqlparser.ast.SQLDataType;
import sjtu.ipads.wtune.sqlparser.ast.constants.Category;
import sjtu.ipads.wtune.testbed.common.BatchActuator;

class UuidConverter implements Converter {
  UuidConverter(SQLDataType dataType) {
    assert dataType.category() == Category.UUID;
  }

  @Override
  public void convert(int seed, BatchActuator actuator) {
    actuator.appendObject(UUID.randomUUID(), Types.OTHER);
  }

  @Override
  public IntStream locate(Object value) {
    throw new NotImplementedException();
  }
}
