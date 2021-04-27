package sjtu.ipads.wtune.testbed.population;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.stream.IntStream;
import org.apache.commons.lang3.NotImplementedException;
import sjtu.ipads.wtune.sqlparser.ast.SQLDataType;
import sjtu.ipads.wtune.sqlparser.ast.constants.Category;
import sjtu.ipads.wtune.sqlparser.ast.constants.DataTypeName;
import sjtu.ipads.wtune.testbed.common.BatchActuator;

class TimeConverter implements Converter {
  private static final LocalDateTime TIME_BASE = LocalDateTime.of(2004, 1, 1, 0, 0, 0);
  private static final LocalDateTime TIME_MIN = LocalDateTime.of(1970, 1, 1, 0, 0, 0);
  private static final LocalDateTime TIME_MAX = LocalDateTime.of(2038, 1, 1, 0, 0, 0);
  private final SQLDataType dataType;

  TimeConverter(SQLDataType dataType) {
    assert dataType.category() == Category.TIME;
    this.dataType = dataType;
  }

  @Override
  public void convert(int seed, BatchActuator actuator) {
    // mysql datatype
    if (dataType.name().equals(DataTypeName.YEAR)) actuator.appendInt(1901 + (seed % 155));
    else {
      switch (dataType.name()) {
        case DataTypeName.DATE:
          actuator.appendDate(coerceIntoRange(TIME_BASE.plus(seed, ChronoUnit.DAYS)).toLocalDate());
          break;
        case DataTypeName.TIME:
        case DataTypeName.TIMETZ:
          actuator.appendTime(
              coerceIntoRange(TIME_BASE.plus(seed, ChronoUnit.SECONDS)).toLocalTime());
          break;
        case DataTypeName.DATETIME:
          actuator.appendDateTime(coerceIntoRange(TIME_BASE.plus(seed, ChronoUnit.SECONDS)));
          break;
        case DataTypeName.TIMESTAMP:
        case DataTypeName.TIMESTAMPTZ:
          actuator.appendDateTime(coerceIntoRange(TIME_BASE.plus(seed, ChronoUnit.MILLIS)));
          break;
      }
    }
  }

  @Override
  public IntStream locate(Object value) {
    throw new NotImplementedException();
  }

  private static LocalDateTime coerceIntoRange(LocalDateTime t) {
    if (t.isAfter(TIME_MAX)) return TIME_MAX;
    if (t.isBefore(TIME_MIN)) return TIME_MIN;
    return t;
  }
}
