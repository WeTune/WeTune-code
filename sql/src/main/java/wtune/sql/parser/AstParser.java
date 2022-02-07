package wtune.sql.parser;

import wtune.sql.ast.SqlNode;
import wtune.sql.mysql.MySQLAstParser;
import wtune.sql.pg.PgAstParser;

import java.util.Properties;

public interface AstParser {
  SqlNode parse(String string);

  default void setProperties(Properties props) {}

  static AstParser ofDb(String dbType) {
    if (SqlNode.MySQL.equals(dbType)) return new MySQLAstParser();
    else if (SqlNode.PostgreSQL.equals(dbType)) return new PgAstParser();
    else throw new IllegalArgumentException();
  }

  static AstParser mysql() {
    return ofDb(SqlNode.MySQL);
  }

  static AstParser postgresql() {
    return ofDb(SqlNode.PostgreSQL);
  }
}
