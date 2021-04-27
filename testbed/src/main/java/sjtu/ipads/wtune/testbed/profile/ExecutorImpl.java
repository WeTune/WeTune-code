package sjtu.ipads.wtune.testbed.profile;

import static sjtu.ipads.wtune.testbed.profile.ParamsGen.IS_NULL;
import static sjtu.ipads.wtune.testbed.profile.ParamsGen.NOT_NULL;
import static sjtu.ipads.wtune.testbed.profile.Profiler.LOG;

import java.lang.System.Logger.Level;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Map;
import sjtu.ipads.wtune.common.utils.ITriConsumer;
import sjtu.ipads.wtune.stmt.resolver.ParamDesc;
import sjtu.ipads.wtune.testbed.common.Actuator;
import sjtu.ipads.wtune.testbed.population.PreparedStatementActuator;

class ExecutorImpl extends PreparedStatementActuator implements Executor {
  private final String sql;
  private final Connection conn;

  private PreparedStatement stmt;
  private ResultSet resultSet;

  ExecutorImpl(Connection conn, String sql) {
    this.conn = conn;
    this.sql = sql;
  }

  @Override
  public boolean installParams(Map<ParamDesc, Object> params) {
    final ParamInstaller installer = new ParamInstaller(this);
    for (var pair : params.entrySet())
      if (!installer.install(pair.getKey(), pair.getValue())) {
        LOG.log(Level.ERROR, "failed to install parameters {0} to sql {1}", pair, sql);
        return false;
      }

    return true;
  }

  @Override
  public boolean execute() {
    try {
      resultSet = statement().executeQuery();
      return true;

    } catch (SQLException exception) {
      LOG.log(
          Level.ERROR,
          "encounter exception when execute query: [{0}] {1}",
          exception.getSQLState(),
          exception.getMessage());
      return false;
    }
  }

  @Override
  public ResultSet getResultSet() {
    return resultSet;
  }

  @Override
  public void endOne() {
    performSQL(stmt::close);
    stmt = null;
    index = 1;
  }

  @Override
  public void close() {
    if (stmt != null) performSQL(stmt::close);
    performSQL(conn::close);
  }

  @Override
  protected Connection connection() {
    return conn;
  }

  @Override
  protected PreparedStatement statement() throws SQLException {
    if (stmt != null) return stmt;
    return stmt = conn.prepareStatement(sql);
  }
}

class ParamInstaller {
  private static final Map<Class<?>, ITriConsumer<ParamInstaller, Integer, Object>> DISPATCH =
      Map.of(
          Integer.class,
          ParamInstaller::setInt,
          Double.class,
          ParamInstaller::setFraction,
          BigDecimal.class,
          ParamInstaller::setDecimal,
          Boolean.class,
          ParamInstaller::setBool,
          String.class,
          ParamInstaller::setString,
          LocalDateTime.class,
          ParamInstaller::setDateTime,
          LocalDate.class,
          ParamInstaller::setDate,
          LocalTime.class,
          ParamInstaller::setTime);

  private final Actuator actuator;

  ParamInstaller(Actuator actuator) {
    this.actuator = actuator;
  }

  private void setInt(int i, Object value) {
    actuator.setInt(i, (Integer) value);
  }

  private void setFraction(int i, Object value) {
    actuator.setFraction(i, (Double) value);
  }

  private void setDecimal(int i, Object value) {
    actuator.setDecimal(i, (BigDecimal) value);
  }

  private void setBool(int i, Object value) {
    actuator.setBool(i, (Boolean) value);
  }

  private void setString(int i, Object value) {
    actuator.setString(i, (String) value);
  }

  private void setDateTime(int i, Object value) {
    actuator.setDateTime(i, (LocalDateTime) value);
  }

  private void setDate(int i, Object value) {
    actuator.setDate(i, (LocalDate) value);
  }

  private void setTime(int i, Object value) {
    actuator.setTime(i, (LocalTime) value);
  }

  public boolean install(ParamDesc param, Object value) {
    if (value != NOT_NULL && value != IS_NULL) {
      final var handle = DISPATCH.get(value.getClass());
      if (handle == null) return false;

      handle.accept(this, param.index() + 1, value);
    }

    return true;
  }
}
