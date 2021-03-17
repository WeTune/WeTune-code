package sjtu.ipads.wtune.superopt.profiler;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import javax.sql.DataSource;

public class DataSourceFactory {
  private static final DataSourceFactory INSTANCE = new DataSourceFactory();
  private final Map<String, DataSource> dataSources = new HashMap<>();

  public static DataSourceFactory instance() {
    return INSTANCE;
  }

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
