package sjtu.ipads.wtune.testbed.population;

import java.sql.SQLException;
import java.sql.Types;
import org.postgresql.util.PGobject;
import sjtu.ipads.wtune.sqlparser.ast.SQLDataType;
import sjtu.ipads.wtune.sqlparser.ast.constants.Category;

class JsonConverter implements Converter {
  JsonConverter(SQLDataType dataType) {
    assert dataType.category() == Category.JSON;
  }

  @Override
  public void convert(int seed, Actuator actuator) {
    try {
      final PGobject obj = new PGobject();

      obj.setType("json");
      obj.setValue("{\"id\": %d}".formatted(seed));

      actuator.appendObject(obj, Types.OTHER);

    } catch (SQLException ex) {
      throw new RuntimeException(ex);
    }
  }
}
