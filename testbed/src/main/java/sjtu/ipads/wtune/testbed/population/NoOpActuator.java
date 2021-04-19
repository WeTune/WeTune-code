package sjtu.ipads.wtune.testbed.population;

import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import sjtu.ipads.wtune.testbed.common.Collection;

public class NoOpActuator implements Actuator {

  @Override
  public void begin(Collection collection) {}

  @Override
  public void end() {}

  @Override
  public void beginOne(Collection collection) {}

  @Override
  public void endOne() {}

  @Override
  public void appendInt(int i) {}

  @Override
  public void appendFraction(double d) {}

  @Override
  public void appendDecimal(BigDecimal d) {}

  @Override
  public void appendBool(boolean b) {}

  @Override
  public void appendString(String s) {}

  @Override
  public void appendDateTime(LocalDateTime t) {}

  @Override
  public void appendTime(LocalTime t) {}

  @Override
  public void appendDate(LocalDate t) {}

  @Override
  public void appendBlob(InputStream in) {}

  @Override
  public void appendBytes(byte[] bs) {}

  @Override
  public void appendObject(Object obj, int typeId) {}

  @Override
  public void appendArray(String type, Object[] array) {}
}
