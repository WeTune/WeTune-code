package sjtu.ipads.wtune.testbed.population;

import static sjtu.ipads.wtune.sqlparser.ast.ASTNode.MYSQL;
import static sjtu.ipads.wtune.sqlparser.ast.ASTNode.POSTGRESQL;
import static sjtu.ipads.wtune.testbed.population.Populator.LOG;

import java.io.InputStream;
import java.lang.System.Logger.Level;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import sjtu.ipads.wtune.testbed.common.Collection;
import sjtu.ipads.wtune.testbed.common.Element;

class DbActuator implements Actuator {
  private final int batchSize;
  private int rowInCurrentBatch;

  private final String dbType;
  private final Connection conn;
  private PreparedStatement stmt;
  private int index;
  private char quotation;

  DbActuator(String dbType, Connection conn, int batchSize) {
    this.dbType = dbType;
    this.conn = conn;
    this.batchSize = batchSize;
    this.quotation = MYSQL.equals(dbType) ? '`' : '"';
  }

  private interface SQLOperation {
    void invoke() throws SQLException;
  }

  private static void performSQL(SQLOperation op) {
    try {
      op.invoke();
    } catch (SQLException throwables) {
      throw new RuntimeException(throwables);
    }
  }

  @Override
  public void begin(Collection collection) {
    performSQL(() -> begin0(collection));
  }

  private void begin0(Collection collection) throws SQLException {
    final Statement stmt = conn.createStatement();
    if (MYSQL.equals(dbType)) {
      stmt.execute("set foreign_key_checks=0");
      stmt.execute("set unique_checks=0");
      stmt.execute("truncate table " + quotation + collection.collectionName() + quotation);

    } else if (POSTGRESQL.equals(dbType)) {
      stmt.execute("set session_replication_role='replica'");
      stmt.execute(
          "truncate table " + quotation + collection.collectionName() + quotation + " CASCADE");
    }

    stmt.close();
  }

  @Override
  public void end() {
    performSQL(this::end0);
  }

  private void end0() throws SQLException {
    if (stmt != null) {
      stmt.executeUpdate();
      stmt = null;
    }
    conn.close();
  }

  @Override
  public void beginOne(Collection collection) {
    LOG.log(Level.TRACE, "begin one for {0}", collection.collectionName());

    performSQL(() -> beginOne0(collection));
  }

  protected void beginOne0(Collection collection) throws SQLException {
    if (rowInCurrentBatch == 0) {
      final List<Element> elements = collection.elements();
      String builder =
          "INSERT INTO "
              + quotation
              + collection.collectionName()
              + quotation
              + " ("
              + elements.stream()
                  .map(it -> "%c%s%c".formatted(quotation, it.elementName(), quotation))
                  .collect(Collectors.joining(","))
              + ") VALUES ("
              + Stream.generate(() -> "?").limit(elements.size()).collect(Collectors.joining(","))
              + ")";

      stmt = conn.prepareStatement(builder);
    }

    index = 1;
    ++rowInCurrentBatch;
  }

  @Override
  public void endOne() {
    LOG.log(Level.TRACE, "end one");
    performSQL(this::endOne0);
  }

  protected void endOne0() throws SQLException {
    stmt.addBatch();
    if (rowInCurrentBatch >= batchSize) {
      stmt.executeUpdate();
      stmt = null;
      rowInCurrentBatch = 0;
    }
  }

  @Override
  public void appendInt(int i) {
    performSQL(() -> stmt.setInt(index++, i));
  }

  @Override
  public void appendFraction(double d) {
    performSQL(() -> stmt.setDouble(index++, d));
  }

  @Override
  public void appendDecimal(BigDecimal d) {
    performSQL(() -> stmt.setBigDecimal(index++, d));
  }

  @Override
  public void appendBool(boolean b) {
    performSQL(() -> stmt.setBoolean(index++, b));
  }

  @Override
  public void appendString(String s) {
    performSQL(() -> stmt.setString(index++, s));
  }

  @Override
  public void appendDateTime(LocalDateTime t) {
    performSQL(() -> stmt.setTimestamp(index++, Timestamp.valueOf(t)));
  }

  @Override
  public void appendTime(LocalTime t) {
    performSQL(() -> stmt.setTime(index++, Time.valueOf(t)));
  }

  @Override
  public void appendDate(LocalDate t) {
    performSQL(() -> stmt.setDate(index++, Date.valueOf(t)));
  }

  @Override
  public void appendBlob(InputStream in) {
    performSQL(() -> stmt.setBlob(index++, in));
  }

  @Override
  public void appendBytes(byte[] bs) {
    performSQL(() -> stmt.setBytes(index++, bs));
  }

  @Override
  public void appendObject(Object obj, int typeId) {
    performSQL(() -> stmt.setObject(index++, obj, typeId));
  }

  @Override
  public void appendArray(String type, Object[] array) {
    performSQL(() -> stmt.setArray(index++, conn.createArrayOf(type, array)));
  }
}
