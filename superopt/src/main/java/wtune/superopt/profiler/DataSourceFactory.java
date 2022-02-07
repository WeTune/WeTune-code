package wtune.superopt.profiler;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class DataSourceFactory {
  private static final DataSourceFactory INSTANCE = new DataSourceFactory();

  private final Map<String, DataSource> dataSources = new HashMap<>();

  private DataSourceFactory() {}

  public static DataSourceFactory instance() {
    return INSTANCE;
  }

  public synchronized DataSource mk(Properties props) {
    return dataSources.computeIfAbsent(
        props.getProperty("jdbcUrl"), ignored -> mkDataSource(props));
  }

  private static DataSource mkDataSource(Properties props) {
    final HikariConfig config = new HikariConfig();
    config.setJdbcUrl(props.getProperty("jdbcUrl"));
    config.setUsername(props.getProperty("username"));
    config.setPassword(props.getProperty("password"));
    return new HikariDataSource(config);
  }
}
