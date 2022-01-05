package sjtu.ipads.wtune.testbed.population;

import org.apache.commons.lang3.NotImplementedException;
import org.postgresql.util.PGobject;
import sjtu.ipads.wtune.sql.ast.SqlDataType;
import sjtu.ipads.wtune.sql.ast.constants.Category;
import sjtu.ipads.wtune.testbed.common.BatchActuator;

import java.sql.SQLException;
import java.sql.Types;
import java.util.stream.IntStream;

class JsonConverter implements Converter {
  JsonConverter(SqlDataType dataType) {
    assert dataType.category() == Category.JSON;
  }

  @Override
  public void convert(int seed, BatchActuator actuator) {
    try {
      final PGobject obj = new PGobject();

      obj.setType("json");
      obj.setValue("{\"id\": %d}".formatted(seed));

      actuator.appendObject(obj, Types.OTHER);

    } catch (SQLException ex) {
      throw new RuntimeException(ex);
    }
  }

  @Override
  public IntStream locate(Object value) {
    throw new NotImplementedException();
  }
}
