package sjtu.ipads.wtune.testbed.population;

import java.io.InputStream;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import sjtu.ipads.wtune.testbed.common.Collection;

public class EchoActuator implements Actuator {
  private final PrintWriter writer;
  private List<String> values;

  public EchoActuator(PrintWriter writer) {
    this.writer = writer;
  }

  @Override
  public void begin(Collection collection) {}

  @Override
  public void end() {}

  @Override
  public void beginOne(Collection collection) {
    this.values = new ArrayList<>(collection.elements().size());
  }

  @Override
  public void endOne() {
    writer.println(String.join(";", values));
    writer.flush();
  }

  @Override
  public void appendInt(int i) {
    values.add(String.valueOf(i));
  }

  @Override
  public void appendFraction(double d) {
    values.add(String.valueOf(d));
  }

  @Override
  public void appendDecimal(BigDecimal d) {
    values.add(String.valueOf(d));
  }

  @Override
  public void appendBool(boolean b) {
    values.add(String.valueOf(b));
  }

  @Override
  public void appendString(String s) {
    values.add("'" + s + "'");
  }

  @Override
  public void appendDateTime(LocalDateTime t) {
    values.add("'" + t + "'");
  }

  @Override
  public void appendTime(LocalTime t) {
    values.add("'" + t + "'");
  }

  @Override
  public void appendDate(LocalDate t) {
    values.add("'" + t + "'");
  }

  @Override
  public void appendBlob(InputStream in) {
    try {
      values.add(Arrays.toString(in.readAllBytes()));

    } catch (Exception ex) {
      values.add("<EXCEPTION>");
    }
  }

  @Override
  public void appendBytes(byte[] bs) {
    values.add(Arrays.toString(bs));
  }

  @Override
  public void appendObject(Object obj, int typeId) {
    values.add(obj.toString());
  }

  @Override
  public void appendArray(String type, Object[] array) {
    values.add(Arrays.toString(array));
  }
}
