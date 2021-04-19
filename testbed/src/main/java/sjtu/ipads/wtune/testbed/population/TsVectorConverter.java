package sjtu.ipads.wtune.testbed.population;

import java.sql.SQLException;
import java.sql.Types;
import org.postgresql.util.PGobject;
import sjtu.ipads.wtune.sqlparser.ast.SQLDataType;
import sjtu.ipads.wtune.sqlparser.ast.constants.DataTypeName;

class TsVectorConverter implements Converter {
  TsVectorConverter(SQLDataType dataType) {
    assert dataType.name().equals(DataTypeName.TSVECTOR);
  }

  @Override
  public void convert(int seed, Actuator actuator) {
    try {
      final PGobject obj = new PGobject();
      obj.setType("tsvector");
      obj.setValue(String.valueOf(seed));
      actuator.appendObject(obj, Types.OTHER);

    } catch (SQLException ex) {
      throw new RuntimeException(ex);
    }
  }
}
