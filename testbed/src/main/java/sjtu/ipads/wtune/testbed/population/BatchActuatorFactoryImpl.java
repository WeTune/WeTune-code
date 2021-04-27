package sjtu.ipads.wtune.testbed.population;

import static sjtu.ipads.wtune.sqlparser.ast.ASTNode.MYSQL;
import static sjtu.ipads.wtune.sqlparser.ast.ASTNode.POSTGRESQL;
import static sjtu.ipads.wtune.testbed.util.DataSourceHelper.makeDataSource;

import java.sql.SQLException;
import java.util.Properties;
import javax.sql.DataSource;
import sjtu.ipads.wtune.testbed.common.BatchActuator;
import sjtu.ipads.wtune.testbed.common.BatchActuatorFactory;

class BatchActuatorFactoryImpl implements BatchActuatorFactory {
  private final Properties dbProperties;
  private final int batchSize;
  private final String dbType;
  private DataSource dataSource;

  BatchActuatorFactoryImpl(Properties properties, int batchSize) {
    this.dbProperties = properties;
    this.batchSize = batchSize;
    this.dbType = determineDbType(properties.getProperty("url"));
  }

  private DataSource dataSource() {
    if (dataSource == null) dataSource = makeDataSource(dbProperties);
    return dataSource;
  }

  private static String determineDbType(String url) {
    if (url.startsWith("jdbc:mysql")) return MYSQL;
    else if (url.startsWith("jdbc:postgresql")) return POSTGRESQL;
    else throw new IllegalArgumentException();
  }

  @Override
  public BatchActuator make(String collectionName) {
    try {
      return new PopulationActuator(dbType, dataSource().getConnection(), batchSize);
    } catch (SQLException ex) {
      throw new RuntimeException(ex);
    }
  }
}
