package sjtu.ipads.wtune.testbed.population;

import org.apache.commons.lang3.NotImplementedException;
import sjtu.ipads.wtune.sql.ast1.SqlDataType;
import sjtu.ipads.wtune.sql.ast1.constants.Category;
import sjtu.ipads.wtune.testbed.common.BatchActuator;

import java.sql.Types;
import java.util.UUID;
import java.util.stream.IntStream;

class UuidConverter implements Converter {
  UuidConverter(SqlDataType dataType) {
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
