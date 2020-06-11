package sjtu.ipads.wtune.sqlparser;

import sjtu.ipads.wtune.sqlparser.mysql.MySQLASTParser;

import static sjtu.ipads.wtune.sqlparser.SQLNode.MYSQL;

public interface SQLParser {
  SQLNode parse(String string);

  static SQLParser ofDb(String dbType) {
    if (MYSQL.equals(dbType)) return new MySQLASTParser();
    else return null;
  }

  static SQLParser mysql() {
    return ofDb(MYSQL);
  }
}
