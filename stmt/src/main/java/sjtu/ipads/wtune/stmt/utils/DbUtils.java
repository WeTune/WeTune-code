package sjtu.ipads.wtune.stmt.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import static sjtu.ipads.wtune.sqlparser.ast.SQLNode.MYSQL;

public class DbUtils {
  private static Connection conn;

  public static Connection connection() {
    try {
      if (conn == null || conn.isClosed())
        synchronized (DbUtils.class) {
          Class.forName("org.sqlite.JDBC");
          if (conn == null || conn.isClosed())
            conn = DriverManager.getConnection("jdbc:sqlite://" + FileUtils.dbPath().toString());
        }

      return conn;

    } catch (SQLException | ClassNotFoundException ex) {
      throw new RuntimeException(ex);
    }
  }

  public static String quoteName(String name, String dbType) {
    if (MYSQL.equals(dbType)) return String.format("`%s`", name);
    else return String.format("\"%s\"", name);
  }
}
