package sjtu.ipads.wtune.testbed;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;
import java.util.logging.LogManager;
import sjtu.ipads.wtune.sqlparser.schema.Schema;
import sjtu.ipads.wtune.sqlparser.schema.Table;
import sjtu.ipads.wtune.stmt.App;
import sjtu.ipads.wtune.testbed.common.Collection;
import sjtu.ipads.wtune.testbed.population.SQLConfig;
import sjtu.ipads.wtune.testbed.population.SQLPopulator;

public class Main {

  private static final String LOGGER_CONFIG =
      ".level = INFO\n"
          + "java.util.logging.ConsoleHandler.level = INFO\n"
          + "handlers=java.util.logging.ConsoleHandler\n"
          + "java.util.logging.ConsoleHandler.formatter=java.util.logging.SimpleFormatter\n"
          + "java.util.logging.SimpleFormatter.format=[%1$tm/%1$td %1$tT][%3$10s][%4$s] %5$s %n\n";

  static {
    try {
      LogManager.getLogManager()
          .readConfiguration(new ByteArrayInputStream(LOGGER_CONFIG.getBytes()));
    } catch (IOException ignored) {
    }
  }

  private static Properties pgProps(String db) {
    final Properties props = new Properties();
    props.setProperty("url", "jdbc:postgresql://10.0.0.102:5432/" + db);
    props.setProperty("username", "zxd");
    return props;
  }

  private static Properties mysqlProps(String db) {
    final Properties props = new Properties();
    props.setProperty("url", "jdbc:mysql://10.0.0.103:3306/" + db);
    props.setProperty("username", "root");
    props.setProperty("password", "admin");
    return props;
  }

  public static void main(String[] args) {
    final String appName = "homeland";
    final Properties dbProps = pgProps(appName + "_base");
    //    final Properties dbProps = mysqlProps(appName + "_base");
    final SQLConfig config = new SQLConfig();
    config.setDbProperties(dbProps);
    config.setBatchSize(1);
    //    config.setDryRun(true);
    //    config.setEcho(false);

    final SQLPopulator populator = new SQLPopulator();
    populator.setConfig(config);

    final App app = App.of(appName);
    final Schema schema = app.schema("base", true);
    final List<Table> list = new ArrayList<>(schema.tables());
    list.sort(Comparator.comparing(Table::name));

    //    final Table table = schema.table("blc_admin_user_sandbox");
    //    populator.populate(Collection.ofTable(table));

    String startPoint = "";
    boolean start = startPoint.isEmpty();

    for (Table table : list) {
      if (!start && startPoint.equals(table.name())) start = true;
      if (!start) continue;

      populator.populate(Collection.ofTable(table));
      //      if (start) break;
    }
  }
}
