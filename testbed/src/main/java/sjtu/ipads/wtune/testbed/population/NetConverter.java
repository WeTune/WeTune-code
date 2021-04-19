package sjtu.ipads.wtune.testbed.population;

import java.sql.SQLException;
import java.sql.Types;
import org.postgresql.util.PGobject;
import sjtu.ipads.wtune.sqlparser.ast.SQLDataType;
import sjtu.ipads.wtune.sqlparser.ast.constants.Category;
import sjtu.ipads.wtune.sqlparser.ast.constants.DataTypeName;

public class NetConverter implements Converter {
  private final boolean isArray;

  public NetConverter(SQLDataType dataType) {
    assert dataType.category() == Category.NET;
    if (!dataType.name().equals(DataTypeName.INET)) throw new UnsupportedOperationException();
    isArray = dataType.dimensions().length > 0;
  }

  @Override
  public void convert(int seed, Actuator actuator) {
    final int _0 = seed & 255;
    final int _1 = (seed >>> 8) & 255;
    final int _2 = (seed >>> 16) & 255;

    try {
      final PGobject obj = new PGobject();
      obj.setType("inet");
      obj.setValue("127.%d.%d.%d".formatted(_2, _1, _0));

      if (isArray) actuator.appendArray("INET", new Object[] {obj});
      else actuator.appendObject(obj, Types.OTHER);

    } catch (SQLException ex) {
      throw new RuntimeException(ex);
    }
  }
}
