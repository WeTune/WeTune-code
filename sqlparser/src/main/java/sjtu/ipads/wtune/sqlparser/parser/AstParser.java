package sjtu.ipads.wtune.sqlparser.parser;

import sjtu.ipads.wtune.sqlparser.ast1.SqlNode;
import sjtu.ipads.wtune.sqlparser.mysql.MySQLAstParser1;
import sjtu.ipads.wtune.sqlparser.pg.PgAstParser1;

import java.util.Properties;

import static sjtu.ipads.wtune.sqlparser.ast.ASTNode.MYSQL;
import static sjtu.ipads.wtune.sqlparser.ast.ASTNode.POSTGRESQL;

public interface AstParser {
  SqlNode parse(String string);

  default void setProperties(Properties props) {}

  static AstParser ofDb(String dbType) {
    if (MYSQL.equals(dbType)) return new MySQLAstParser1();
    else if (POSTGRESQL.equals(dbType)) return new PgAstParser1();
    else throw new IllegalArgumentException();
  }

  static AstParser mysql() {
    return ofDb(MYSQL);
  }

  static AstParser postgresql() {
    return ofDb(POSTGRESQL);
  }
}
