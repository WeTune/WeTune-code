package sjtu.ipads.wtune.superopt.profiler;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class DataSourceFactory {
  private final Map<String, DataSource> dataSources = new HashMap<>();

  public DataSource make(Properties props) {
    return dataSources.computeIfAbsent(
        props.getProperty("jdbcUrl"), ignored -> makeDataSource(props));
  }

  private static DataSource makeDataSource(Properties props) {
    final HikariConfig config = new HikariConfig();
    final String url = props.getProperty("jdbcUrl");
    config.setJdbcUrl(url);
    config.setUsername(props.getProperty("username"));
    config.setPassword(props.getProperty("password"));
    return new HikariDataSource(config);
  }
}
