package sjtu.ipads.wtune.testbed.population;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import sjtu.ipads.wtune.sqlparser.ast.SQLDataType;
import sjtu.ipads.wtune.sqlparser.ast.constants.Category;
import sjtu.ipads.wtune.sqlparser.ast.constants.DataTypeName;

class TimeConverter implements Converter {
  private static final LocalDateTime TIME_BASE = LocalDateTime.of(2021, 1, 1, 0, 0, 0);
  private static final LocalDateTime TIME_MIN = LocalDateTime.of(1970, 1, 1, 0, 0, 0);
  private static final LocalDateTime TIME_MAX = LocalDateTime.of(2038, 1, 1, 0, 0, 0);
  private final SQLDataType dataType;

  TimeConverter(SQLDataType dataType) {
    assert dataType.category() == Category.TIME;
    this.dataType = dataType;
  }

  @Override
  public void convert(int seed, Actuator actuator) {
    // mysql datatype
    if (dataType.name().equals(DataTypeName.YEAR)) actuator.appendInt(1901 + (seed % 155));
    else {
      LocalDateTime datetime = TIME_BASE.plus(seed, ChronoUnit.SECONDS);
      if (datetime.isAfter(TIME_MAX)) datetime = TIME_MAX;
      if (datetime.isBefore(TIME_MIN)) datetime = TIME_MIN;

      switch (dataType.name()) {
        case DataTypeName.DATE:
          actuator.appendDate(datetime.toLocalDate());
          break;
        case DataTypeName.TIME:
          actuator.appendTime(datetime.toLocalTime());
          break;
        default:
          actuator.appendDateTime(datetime);
      }
    }
  }
}
