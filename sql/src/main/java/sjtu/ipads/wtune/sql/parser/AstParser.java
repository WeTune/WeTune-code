package sjtu.ipads.wtune.sql.parser;

import sjtu.ipads.wtune.sql.ast.SqlNode;
import sjtu.ipads.wtune.sql.mysql.MySQLAstParser;
import sjtu.ipads.wtune.sql.pg.PgAstParser;

import java.util.Properties;

import static sjtu.ipads.wtune.sql.ast.SqlNode.MySQL;
import static sjtu.ipads.wtune.sql.ast.SqlNode.PostgreSQL;

public interface AstParser {
  SqlNode parse(String string);

  default void setProperties(Properties props) {}

  static AstParser ofDb(String dbType) {
    if (MySQL.equals(dbType)) return new MySQLAstParser();
    else if (PostgreSQL.equals(dbType)) return new PgAstParser();
    else throw new IllegalArgumentException();
  }

  static AstParser mysql() {
    return ofDb(MySQL);
  }

  static AstParser postgresql() {
    return ofDb(PostgreSQL);
  }
}
