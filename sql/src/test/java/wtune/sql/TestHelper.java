package wtune.sql;

import wtune.common.utils.Lazy;
import wtune.sql.ast.SqlNode;
import wtune.sql.schema.Schema;

public class TestHelper {
  private static final String TEST_SCHEMA =
      ""
          + "CREATE TABLE a ( i INT PRIMARY KEY, j INT, k INT );"
          + "CREATE TABLE b ( x INT PRIMARY KEY, y INT, z INT );"
          + "CREATE TABLE c ( u INT PRIMARY KEY, v CHAR(10), w DECIMAL(1, 10) );"
          + "CREATE TABLE d ( p INT, q CHAR(10), r DECIMAL(1, 10), UNIQUE KEY (p), FOREIGN KEY (p) REFERENCES c (u) );";
  private static final Lazy<Schema> SCHEMA =
      Lazy.mk(() -> SqlSupport.parseSchema(DbSupport.MySQL, TEST_SCHEMA));

  public static SqlNode parseSql(String sql) {
    final SqlNode ast = SqlSupport.parseSql(DbSupport.MySQL, sql);
    ast.context().setSchema(SCHEMA.get());
    return ast;
  }
}
