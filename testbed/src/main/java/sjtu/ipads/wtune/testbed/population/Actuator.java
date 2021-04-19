package sjtu.ipads.wtune.testbed.population;

import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import sjtu.ipads.wtune.testbed.common.Collection;

public interface Actuator {
  void begin(Collection collection);

  void end();

  void beginOne(Collection collection);

  void endOne();

  void appendInt(int i);

  void appendFraction(double d);

  void appendDecimal(BigDecimal d);

  void appendBool(boolean b);

  void appendString(String s);

  void appendDateTime(LocalDateTime t);

  void appendTime(LocalTime t);

  void appendDate(LocalDate t);

  void appendBlob(InputStream in);

  void appendBytes(byte[] bs);

  void appendObject(Object obj, int typeId);

  void appendArray(String type, Object[] array);
}
