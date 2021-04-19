package sjtu.ipads.wtune.testbed.population;

import static sjtu.ipads.wtune.sqlparser.ast.ASTNode.MYSQL;
import static sjtu.ipads.wtune.sqlparser.ast.ASTNode.POSTGRESQL;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.SQLException;
import java.util.Properties;
import javax.sql.DataSource;

public class DbActuatorFactory implements ActuatorFactory {
  private final Properties dbProperties;
  private final int batchSize;
  private final String dbType;
  private DataSource dataSource;

  public DbActuatorFactory(Properties properties, int batchSize) {
    this.dbProperties = properties;
    this.batchSize = batchSize;
    this.dbType = determineDbType(properties.getProperty("url"));
  }

  private DataSource dataSource() {
    if (dataSource != null) return dataSource;
    final HikariConfig config = new HikariConfig();
    config.setJdbcUrl(dbProperties.getProperty("url"));
    config.setUsername(dbProperties.getProperty("username"));
    config.setPassword(dbProperties.getProperty("password"));
    config.setMaximumPoolSize(1);
    return dataSource = new HikariDataSource(config);
  }

  private static String determineDbType(String url) {
    if (url.startsWith("jdbc:mysql")) return MYSQL;
    else if (url.startsWith("jdbc:postgresql")) return POSTGRESQL;
    else throw new IllegalArgumentException();
  }

  @Override
  public Actuator get() {
    try {
      return new DbActuator(dbType, dataSource().getConnection(), batchSize);
    } catch (SQLException ex) {
      throw new RuntimeException(ex);
    }
  }
}
