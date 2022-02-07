package wtune.testbed.profile;

import com.zaxxer.hikari.HikariDataSource;
import wtune.testbed.util.DataSourceSupport;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.Properties;

class ExecutorFactoryImpl implements ExecutorFactory {
  private final Properties dbProperties;
  private DataSource dataSource;

  ExecutorFactoryImpl(Properties dbProperties) {
    this.dbProperties = dbProperties;
  }

  private DataSource dataSource() {
    if (dataSource == null) dataSource = DataSourceSupport.makeDataSource(dbProperties);
    return dataSource;
  }

  @Override
  public Executor mk(String sql, boolean useSqlServer) {
    try {
      if (useSqlServer){ // need syntax transform in ExecutorSQLServerImpl
        return new ExecutorSQLServerImpl(dataSource().getConnection(), sql);
      }
      return new ExecutorImpl(dataSource().getConnection(), sql);
    } catch (SQLException ex) {
      throw new RuntimeException(ex);
    }
  }

  @Override
  public void close() {
    if (dataSource != null) {
      try {
        dataSource.unwrap(HikariDataSource.class).close();
      } catch (SQLException exception) {
        throw new RuntimeException(exception);
      }
    }
  }
}
