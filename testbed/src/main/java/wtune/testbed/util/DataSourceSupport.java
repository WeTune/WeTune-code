package wtune.testbed.util;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;
import java.util.Properties;

public interface DataSourceSupport {
  static DataSource makeDataSource(Properties dbProps) {
    final HikariConfig config = new HikariConfig();
    config.setJdbcUrl(dbProps.getProperty("url"));
    config.setUsername(dbProps.getProperty("username"));
    config.setPassword(dbProps.getProperty("password"));
    config.setMaximumPoolSize(2);
    return new HikariDataSource(config);
  }

  static Properties pgProps(String db) {
    final Properties props = new Properties();
    props.setProperty("url", "jdbc:postgresql://10.0.0.103:5432/" + db);
    props.setProperty("username", "root");
    props.setProperty("password", "admin");
    return props;
  }

  static Properties mysqlProps(String db) {
    final Properties props = new Properties();
    props.setProperty(
        "url", "jdbc:mysql://10.0.0.103:3306/" + db + "?rewriteBatchedStatements=true");
    props.setProperty("username", "root");
    props.setProperty("password", "admin");
    return props;
  }

  static Properties sqlserverProps(String db) {
    final Properties props = new Properties();
    props.setProperty(
        "url", "jdbc:sqlserver://localhost:1433;DatabaseName=" + db);
    props.setProperty("username", "SA");
    props.setProperty("password", "mssql2019Admin");
    return props;
  }
}
